import { InkCanvas } from './canvas.js';
import { AutoPushEngine } from './autoPushEngine.js';
import { GeminiEngine } from './geminiEngine.js';
import { ActionHub } from './actionHub.js';
import { StorageService } from './storage.js';

class InkActionApp {
  constructor() {
    this.canvas = null;
    this.autoPush = null;
    this.gemini = null;
    this.actionHub = null;

    this.init();
  }

  init() {
    this.applyTheme(StorageService.getTheme());
    this.initGemini();
    this.initActionHub();
    this.initAutoPush();
    this.initCanvas();
    this.bindUI();
    this.bindShortcuts();
    this.bindToasts();
  }

  applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    StorageService.setTheme(theme);
  }

  initGemini() {
    const apiKey = StorageService.getApiKey();
    const model = StorageService.getModel();
    this.gemini = new GeminiEngine(apiKey, model);
  }

  initActionHub() {
    const container = document.getElementById('actions-pane');
    this.actionHub = new ActionHub(container, {
      onTodoToggle: (todos) => {
        // Update session in storage
        const currentData = this.actionHub.currentData;
        if (currentData) {
          currentData.todos = todos;
        }
      }
    });

    this.gemini.setStatusCallback((status) => {
      this.actionHub.updateStatus(status);
    });
  }

  initAutoPush() {
    const debounceMs = StorageService.getDebounceMs();
    
    this.autoPush = new AutoPushEngine({
      delayMs: debounceMs,
      enabled: debounceMs > 0,
      onTick: ({ progress, remainingSec }) => {
        this.updateCountdownUI(progress, remainingSec);
      },
      onStateChange: ({ armed, processing }) => {
        const pill = document.getElementById('autopush-pill');
        const text = document.getElementById('autopush-text');
        const pushBtn = document.getElementById('btn-push-action');

        if (pill && text) {
          pill.classList.toggle('armed', armed);
          if (processing) {
            text.textContent = 'Processing with Gemini...';
            if (pushBtn) pushBtn.disabled = true;
          } else if (armed) {
            text.textContent = `Auto-pushing in countdown...`;
            if (pushBtn) pushBtn.disabled = false;
          } else {
            text.textContent = 'Auto-push on finish';
            if (pushBtn) pushBtn.disabled = false;
          }
        }
      },
      onTrigger: async () => {
        await this.processInk();
      }
    });
  }

  initCanvas() {
    const canvasEl = document.getElementById('ink-canvas');
    const savedColor = StorageService.getPenColor();
    const savedSize = StorageService.getPenSize();

    this.canvas = new InkCanvas(canvasEl, {
      penColor: savedColor,
      penSize: savedSize,
      onStrokeStart: () => {
        this.autoPush.onStrokeStart();
      },
      onStrokeEnd: (strokeCount) => {
        this.autoPush.onStrokeEnd(strokeCount);
      }
    });
  }

  async processInk() {
    if (this.canvas.isEmpty()) {
      this.showToast('Write something with your pen first!');
      return;
    }

    const base64Image = this.canvas.getSnapshotBase64();
    if (!base64Image) return;

    try {
      // Switch view on mobile to Action Hub so user can see AI results immediately
      if (window.innerWidth <= 768) {
        this.setActiveView('actions');
      }

      const result = await this.gemini.processHandwrittenNotes(base64Image);
      this.actionHub.setData(result);
      
      // Save session
      StorageService.saveSession({
        id: `session-${Date.now()}`,
        timestamp: new Date().toISOString(),
        data: result
      });

      this.showToast('Notes & actions successfully generated!');
    } catch (err) {
      console.error(err);
      this.showToast('Failed to process notes. Check API key in settings.', 'error');
    }
  }

  updateCountdownUI(progress, remainingSec) {
    const circle = document.getElementById('progress-circle');
    const text = document.getElementById('autopush-text');
    if (circle) {
      const circumference = 2 * Math.PI * 8; // r=8 -> 50.26
      const offset = circumference * (1 - progress);
      circle.style.strokeDashoffset = offset;
    }
    if (text && progress > 0 && remainingSec > 0) {
      text.textContent = `Auto-push in ${remainingSec}s...`;
    }
  }

  bindUI() {
    // Toolbar - Tools (Pen, Highlighter, Eraser)
    const toolBtns = document.querySelectorAll('.tool-btn[data-tool]');
    toolBtns.forEach(btn => {
      btn.addEventListener('click', () => {
        toolBtns.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        const tool = btn.getAttribute('data-tool');
        this.canvas.setTool(tool);
      });
    });

    // Toolbar - Color picker swatches
    const colorDots = document.querySelectorAll('.color-dot');
    colorDots.forEach(dot => {
      dot.addEventListener('click', () => {
        colorDots.forEach(d => d.classList.remove('active'));
        dot.classList.add('active');
        const color = dot.getAttribute('data-color');
        this.canvas.setColor(color);
        StorageService.setPenColor(color);
      });
    });

    // Undo / Redo / Clear
    document.getElementById('btn-undo')?.addEventListener('click', () => this.canvas.undo());
    document.getElementById('btn-redo')?.addEventListener('click', () => this.canvas.redo());
    document.getElementById('btn-clear')?.addEventListener('click', () => {
      if (confirm('Clear entire handwritten canvas?')) {
        this.canvas.clear();
        this.autoPush.cancel();
      }
    });

    // Manual "Actionize / Push" button
    document.getElementById('btn-push-action')?.addEventListener('click', () => {
      this.autoPush.trigger();
    });

    // Settings Modal
    const settingsModal = document.getElementById('settings-modal');
    const btnOpenSettings = document.getElementById('btn-open-settings');
    const btnCloseSettings = document.getElementById('btn-close-settings');
    const btnSaveSettings = document.getElementById('btn-save-settings');

    btnOpenSettings?.addEventListener('click', () => {
      document.getElementById('input-api-key').value = StorageService.getApiKey();
      document.getElementById('select-model').value = StorageService.getModel();
      document.getElementById('select-debounce').value = StorageService.getDebounceMs().toString();
      settingsModal.classList.add('active');
    });

    btnCloseSettings?.addEventListener('click', () => {
      settingsModal.classList.remove('active');
    });

    btnSaveSettings?.addEventListener('click', () => {
      const apiKey = document.getElementById('input-api-key').value.trim();
      const model = document.getElementById('select-model').value;
      const debounceMs = parseInt(document.getElementById('select-debounce').value, 10);

      StorageService.setApiKey(apiKey);
      StorageService.setModel(model);
      StorageService.setDebounceMs(debounceMs);

      this.gemini.setApiKey(apiKey);
      this.gemini.setModel(model);
      this.autoPush.setDelay(debounceMs);
      this.autoPush.setEnabled(debounceMs > 0);

      settingsModal.classList.remove('active');
      this.showToast('Settings saved!');
    });

    // Theme Toggle
    const btnThemeToggle = document.getElementById('btn-theme-toggle');
    btnThemeToggle?.addEventListener('click', () => {
      const current = document.documentElement.getAttribute('data-theme') || 'dark';
      const next = current === 'dark' ? 'light' : 'dark';
      this.applyTheme(next);
    });

    // Mobile View switcher (Canvas vs Actions on phones like S26 Ultra)
    const viewTabs = document.querySelectorAll('.view-tab-btn');
    viewTabs.forEach(btn => {
      btn.addEventListener('click', () => {
        const view = btn.getAttribute('data-view');
        this.setActiveView(view);
      });
    });
  }

  setActiveView(view) {
    const container = document.getElementById('workspace');
    if (container) {
      container.setAttribute('data-active-view', view);
    }
    document.querySelectorAll('.view-tab-btn').forEach(b => {
      b.classList.toggle('active', b.getAttribute('data-view') === view);
    });
  }

  bindShortcuts() {
    window.addEventListener('keydown', (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'z') {
        e.preventDefault();
        if (e.shiftKey) {
          this.canvas.redo();
        } else {
          this.canvas.undo();
        }
      } else if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'y') {
        e.preventDefault();
        this.canvas.redo();
      } else if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'enter') {
        e.preventDefault();
        this.autoPush.trigger();
      }
    });
  }

  bindToasts() {
    window.addEventListener('inkaction:toast', (e) => {
      this.showToast(e.detail.text, e.detail.type);
    });
  }

  showToast(message, type = 'success') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    container.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateY(10px)';
      toast.style.transition = 'all 0.2s ease';
      setTimeout(() => toast.remove(), 200);
    }, 3000);
  }
}

// Bootstrap Application
window.addEventListener('DOMContentLoaded', () => {
  window.inkAction = new InkActionApp();
});
