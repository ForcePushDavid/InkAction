/**
 * InkAction Action Hub
 * Displays and coordinates multi-agent output: Synthesized Notes,
 * Interactive Todo list with filters, Calendar view with Google Calendar links & iCal export.
 */
export class ActionHub {
  constructor(containerElement, options = {}) {
    this.container = containerElement;
    this.options = options;
    this.currentData = null;
    this.activeTab = 'notes'; // 'notes' | 'todos' | 'calendar' | 'graph'
    this.todos = [];
    this.events = [];
    this.note = null;
    this.topics = [];

    this.onTodoToggle = options.onTodoToggle || (() => {});
    this.init();
  }

  init() {
    this.renderSkeleton();
    this.bindEvents();
  }

  renderSkeleton() {
    this.container.innerHTML = `
      <div class="actions-header">
        <nav class="actions-nav" role="tablist">
          <button class="nav-tab-btn active" data-tab="notes" id="tab-btn-notes">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
            Notes <span class="tab-badge" id="notes-badge">0</span>
          </button>
          <button class="nav-tab-btn" data-tab="todos" id="tab-btn-todos">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 11 12 14 22 4"></polyline><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"></path></svg>
            Todos <span class="tab-badge" id="todos-badge">0</span>
          </button>
          <button class="nav-tab-btn" data-tab="calendar" id="tab-btn-calendar">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
            Calendar <span class="tab-badge" id="events-badge">0</span>
          </button>
          <button class="nav-tab-btn" data-tab="graph" id="tab-btn-graph">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="18" cy="5" r="3"></circle><circle cx="6" cy="12" r="3"></circle><circle cx="18" cy="19" r="3"></circle><line x1="8.59" y1="13.51" x2="15.42" y2="17.49"></line><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"></line></svg>
            Links
          </button>
        </nav>
        <div class="actions-header-tools">
          <button class="btn btn-icon" id="btn-export-all" title="Export Markdown / ICS" aria-label="Export Data">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
          </button>
        </div>
      </div>

      <!-- Agent Pipeline Progress / Status Bar -->
      <div class="agent-status-bar idle" id="agent-status-bar">
        <div class="agent-steps" id="agent-steps">
          <span class="agent-chip" id="chip-synthesizer">📝 Synthesizer</span>
          <span class="agent-chip" id="chip-todo">✅ Action Extractor</span>
          <span class="agent-chip" id="chip-calendar">📅 Scheduler</span>
        </div>
        <div class="agent-status-text" id="agent-status-text">Ready for pen notes</div>
      </div>

      <!-- Main Action Content -->
      <div class="actions-content" id="actions-content">
        <!-- Notes Section -->
        <section class="action-view-section active" id="view-notes">
          <div class="empty-state" id="empty-notes">
            <svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 19l7-7 3 3-7 7-3-3z"></path><path d="M18 13l-1.5-7.5L2 2l3.5 14.5L13 18l5-5z"></path><path d="M2 2l7.586 7.586"></path><circle cx="11" cy="11" r="2"></circle></svg>
            <div class="empty-title">Write with your S-Pen or Stylus</div>
            <div class="empty-desc">When you finish writing, InkAction's Google Gemini agents will automatically synthesize clean markdown, checklists, and calendar events here.</div>
          </div>
          <div id="notes-container"></div>
        </section>

        <!-- Todos Section -->
        <section class="action-view-section" id="view-todos">
          <div class="empty-state" id="empty-todos">
            <svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="9 11 12 14 22 4"></polyline><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"></path></svg>
            <div class="empty-title">No pending action items</div>
            <div class="empty-desc">Scribble tasks, priorities, or checkboxes. InkAction extracts and organizes them with deadlines.</div>
          </div>
          <div class="todo-list-container" id="todos-container"></div>
        </section>

        <!-- Calendar Section -->
        <section class="action-view-section" id="view-calendar">
          <div class="empty-state" id="empty-calendar">
            <svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
            <div class="empty-title">No events scheduled</div>
            <div class="empty-desc">Mention dates, meetings, or reminders in your handwriting to generate instant Google Calendar links and .ics invites.</div>
          </div>
          <div class="calendar-view-container" id="calendar-container"></div>
        </section>

        <!-- Graph Links Section -->
        <section class="action-view-section" id="view-graph">
          <div class="empty-state" id="empty-graph">
            <svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="18" cy="5" r="3"></circle><circle cx="6" cy="12" r="3"></circle><circle cx="18" cy="19" r="3"></circle><line x1="8.59" y1="13.51" x2="15.42" y2="17.49"></line><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"></line></svg>
            <div class="empty-title">Knowledge Graph & Topic Links</div>
            <div class="empty-desc">Concepts from your handwriting are tagged and cross-linked across notes automatically.</div>
          </div>
          <div class="graph-view-container" id="graph-container"></div>
        </section>
      </div>
    `;
  }

  bindEvents() {
    const navButtons = this.container.querySelectorAll('.nav-tab-btn');
    navButtons.forEach(btn => {
      btn.addEventListener('click', () => {
        const tab = btn.getAttribute('data-tab');
        this.switchTab(tab);
      });
    });

    const exportBtn = this.container.querySelector('#btn-export-all');
    if (exportBtn) {
      exportBtn.addEventListener('click', () => this.exportCurrentSession());
    }
  }

  switchTab(tab) {
    this.activeTab = tab;
    this.container.querySelectorAll('.nav-tab-btn').forEach(btn => {
      btn.classList.toggle('active', btn.getAttribute('data-tab') === tab);
    });
    this.container.querySelectorAll('.action-view-section').forEach(sec => {
      sec.classList.toggle('active', sec.id === `view-${tab}`);
    });
  }

  updateStatus(status) {
    const bar = this.container.querySelector('#agent-status-bar');
    const text = this.container.querySelector('#agent-status-text');
    const chipSynth = this.container.querySelector('#chip-synthesizer');
    const chipTodo = this.container.querySelector('#chip-todo');
    const chipCal = this.container.querySelector('#chip-calendar');

    if (!bar || !text) return;

    text.textContent = status.message || 'Processing...';

    if (status.step === 'idle') {
      bar.className = 'agent-status-bar idle';
      chipSynth.className = 'agent-chip';
      chipTodo.className = 'agent-chip';
      chipCal.className = 'agent-chip';
    } else if (status.step === 'uploading' || status.step === 'synthesizing') {
      bar.className = 'agent-status-bar';
      chipSynth.className = 'agent-chip active';
      chipTodo.className = 'agent-chip';
      chipCal.className = 'agent-chip';
    } else if (status.step === 'extracting') {
      bar.className = 'agent-status-bar';
      chipSynth.className = 'agent-chip done';
      chipTodo.className = 'agent-chip active';
      chipCal.className = 'agent-chip active';
    } else if (status.step === 'done') {
      bar.className = 'agent-status-bar';
      chipSynth.className = 'agent-chip done';
      chipTodo.className = 'agent-chip done';
      chipCal.className = 'agent-chip done';
      setTimeout(() => {
        bar.className = 'agent-status-bar idle';
        text.textContent = 'All actions synced';
      }, 3500);
    }
  }

  /**
   * Updates Action Hub with synthesized output from Gemini
   */
  setData(data) {
    this.currentData = data;
    this.note = data.note || null;
    this.todos = data.todos || [];
    this.events = data.events || [];
    this.topics = data.topics || [];

    this.renderNotes();
    this.renderTodos();
    this.renderCalendar();
    this.renderGraph();
    this.updateBadges();
  }

  updateBadges() {
    const notesBadge = this.container.querySelector('#notes-badge');
    const todosBadge = this.container.querySelector('#todos-badge');
    const eventsBadge = this.container.querySelector('#events-badge');

    if (notesBadge) notesBadge.textContent = this.note ? '1' : '0';
    if (todosBadge) {
      const activeCount = this.todos.filter(t => !t.completed).length;
      todosBadge.textContent = activeCount.toString();
    }
    if (eventsBadge) eventsBadge.textContent = this.events.length.toString();
  }

  renderNotes() {
    const container = this.container.querySelector('#notes-container');
    const empty = this.container.querySelector('#empty-notes');
    if (!container || !empty) return;

    if (!this.note) {
      empty.style.display = 'flex';
      container.innerHTML = '';
      return;
    }

    empty.style.display = 'none';
    const formattedMarkdown = this.formatMarkdown(this.note.markdown);

    container.innerHTML = `
      <article class="note-card">
        <header class="note-header">
          <div>
            <h2 class="note-title">${this.escapeHtml(this.note.title)}</h2>
            <div class="note-timestamp">Synthesized ${new Date().toLocaleTimeString()}</div>
          </div>
          <button class="btn btn-icon" id="btn-copy-note" title="Copy Markdown" aria-label="Copy Note Markdown">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>
          </button>
        </header>

        ${this.note.summary ? `<div class="note-summary">${this.escapeHtml(this.note.summary)}</div>` : ''}

        <div class="note-body">
          ${formattedMarkdown}
        </div>

        ${this.note.tags && this.note.tags.length > 0 ? `
          <div class="note-tags">
            ${this.note.tags.map(t => `<span class="tag-badge">#${this.escapeHtml(t)}</span>`).join('')}
          </div>
        ` : ''}
      </article>
    `;

    const copyBtn = container.querySelector('#btn-copy-note');
    if (copyBtn) {
      copyBtn.addEventListener('click', () => {
        const fullText = `# ${this.note.title}\n\n> ${this.note.summary}\n\n${this.note.markdown}`;
        navigator.clipboard.writeText(fullText);
        window.dispatchEvent(new CustomEvent('inkaction:toast', { detail: { text: 'Note copied to clipboard!' } }));
      });
    }
  }

  renderTodos() {
    const container = this.container.querySelector('#todos-container');
    const empty = this.container.querySelector('#empty-todos');
    if (!container || !empty) return;

    if (!this.todos || this.todos.length === 0) {
      empty.style.display = 'flex';
      container.innerHTML = '';
      return;
    }

    empty.style.display = 'none';
    container.innerHTML = this.todos.map((todo) => `
      <div class="todo-item ${todo.completed ? 'completed' : ''}" data-id="${todo.id}">
        <input type="checkbox" class="todo-checkbox" ${todo.completed ? 'checked' : ''} aria-label="Toggle task">
        <div class="todo-content">
          <div class="todo-text">${this.escapeHtml(todo.text)}</div>
          <div class="todo-meta">
            <span class="priority-badge priority-${todo.priority}">${todo.priority}</span>
            ${todo.dueDate && todo.dueDate !== 'None' ? `
              <span class="due-date-pill">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
                ${this.escapeHtml(todo.dueDate)}
              </span>
            ` : ''}
          </div>
        </div>
      </div>
    `).join('');

    container.querySelectorAll('.todo-checkbox').forEach((checkbox, idx) => {
      checkbox.addEventListener('change', (e) => {
        this.todos[idx].completed = e.target.checked;
        const item = checkbox.closest('.todo-item');
        if (item) item.classList.toggle('completed', e.target.checked);
        this.updateBadges();
        this.onTodoToggle(this.todos);
      });
    });
  }

  renderCalendar() {
    const container = this.container.querySelector('#calendar-container');
    const empty = this.container.querySelector('#empty-calendar');
    if (!container || !empty) return;

    if (!this.events || this.events.length === 0) {
      empty.style.display = 'flex';
      container.innerHTML = '';
      return;
    }

    empty.style.display = 'none';
    container.innerHTML = this.events.map(ev => {
      const gcalUrl = this.generateGoogleCalendarLink(ev);
      const dateObj = new Date(ev.date || Date.now());
      const dayStr = dateObj.toLocaleDateString(undefined, { weekday: 'short' });
      const dayNum = dateObj.getDate() || '1';

      return `
        <div class="event-card">
          <div class="event-time-block">
            <span class="event-day">${dayStr}</span>
            <span class="event-date-num">${dayNum}</span>
            <span class="event-time">${this.escapeHtml(ev.time || 'All Day')}</span>
          </div>
          <div class="event-details">
            <h3 class="event-title">${this.escapeHtml(ev.title)}</h3>
            ${ev.description ? `<p class="event-description">${this.escapeHtml(ev.description)}</p>` : ''}
            <div class="event-actions">
              <a href="${gcalUrl}" target="_blank" rel="noopener noreferrer" class="btn-gcal">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path><polyline points="15 3 21 3 21 9"></polyline><line x1="10" y1="14" x2="21" y2="3"></line></svg>
                Google Calendar
              </a>
              <button class="btn btn-icon btn-download-ics" data-id="${ev.id}" title="Download .ics" aria-label="Download iCal">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
              </button>
            </div>
          </div>
        </div>
      `;
    }).join('');

    container.querySelectorAll('.btn-download-ics').forEach(btn => {
      btn.addEventListener('click', () => {
        const id = btn.getAttribute('data-id');
        const ev = this.events.find(e => e.id === id);
        if (ev) this.downloadICS(ev);
      });
    });
  }

  renderGraph() {
    const container = this.container.querySelector('#graph-container');
    const empty = this.container.querySelector('#empty-graph');
    if (!container || !empty) return;

    if (!this.topics || this.topics.length === 0) {
      empty.style.display = 'flex';
      container.innerHTML = '';
      return;
    }

    empty.style.display = 'none';
    container.innerHTML = this.topics.map(t => `
      <div class="graph-topic-card">
        <div class="topic-header">
          <span class="topic-name"># ${this.escapeHtml(t.name)}</span>
        </div>
        <div class="topic-connections">${this.escapeHtml(t.summary)}</div>
      </div>
    `).join('');
  }

  generateGoogleCalendarLink(ev) {
    const title = encodeURIComponent(ev.title || 'InkAction Event');
    const details = encodeURIComponent(ev.description || 'Created via InkAction AI');
    const location = encodeURIComponent(ev.location || '');
    
    // Format date string for Google Calendar (YYYYMMDDTHHmmSSZ or YYYYMMDD)
    const dateStr = (ev.date || new Date().toISOString().split('T')[0]).replace(/-/g, '');
    const timeClean = (ev.time || '10:00').replace(/[^0-9]/g, '');
    const paddedTime = (timeClean + '0000').slice(0, 4) + '00';
    const startIso = `${dateStr}T${paddedTime}`;
    const endIso = `${dateStr}T${paddedTime}`;

    return `https://calendar.google.com/calendar/render?action=TEMPLATE&text=${title}&details=${details}&location=${location}&dates=${startIso}/${endIso}`;
  }

  downloadICS(ev) {
    const dateStr = (ev.date || new Date().toISOString().split('T')[0]).replace(/-/g, '');
    const icsContent = [
      'BEGIN:VCALENDAR',
      'VERSION:2.0',
      'PRODID:-//InkAction//AI Note Action//EN',
      'BEGIN:VEVENT',
      `SUMMARY:${ev.title}`,
      `DESCRIPTION:${ev.description || ''}`,
      `DTSTART:${dateStr}T090000Z`,
      `DTEND:${dateStr}T100000Z`,
      'END:VEVENT',
      'END:VCALENDAR'
    ].join('\r\n');

    const blob = new Blob([icsContent], { type: 'text/calendar;charset=utf-8' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `${(ev.title || 'event').toLowerCase().replace(/[^a-z0-9]/g, '-')}.ics`;
    link.click();
  }

  exportCurrentSession() {
    if (!this.note) {
      window.dispatchEvent(new CustomEvent('inkaction:toast', { detail: { text: 'No notes to export yet!' } }));
      return;
    }

    let fullExport = `# ${this.note.title}\n\n`;
    if (this.note.summary) fullExport += `> ${this.note.summary}\n\n`;
    fullExport += `## Notes\n${this.note.markdown}\n\n`;

    if (this.todos.length > 0) {
      fullExport += `## Action Items\n`;
      this.todos.forEach(t => {
        fullExport += `- [${t.completed ? 'x' : ' '}] ${t.text} (${t.priority.toUpperCase()}${t.dueDate ? ' - ' + t.dueDate : ''})\n`;
      });
      fullExport += '\n';
    }

    if (this.events.length > 0) {
      fullExport += `## Calendar Events\n`;
      this.events.forEach(e => {
        fullExport += `- **${e.title}**: ${e.date} at ${e.time} (${e.duration})\n  ${e.description}\n`;
      });
    }

    const blob = new Blob([fullExport], { type: 'text/markdown;charset=utf-8' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `${this.note.title.toLowerCase().replace(/[^a-z0-9]/g, '-')}.md`;
    link.click();

    window.dispatchEvent(new CustomEvent('inkaction:toast', { detail: { text: 'Exported Markdown summary!' } }));
  }

  formatMarkdown(md) {
    if (!md) return '';
    return md
      .replace(/^### (.*$)/gim, '<h4>$1</h4>')
      .replace(/^## (.*$)/gim, '<h3>$1</h3>')
      .replace(/^# (.*$)/gim, '<h2>$1</h2>')
      .replace(/\*\*(.*?)\*\*/gim, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/gim, '<em>$1</em>')
      .replace(/^\* (.*$)/gim, '<li>$1</li>')
      .replace(/^[0-9]\. (.*$)/gim, '<li>$1</li>')
      .replace(/\n\n/gim, '<br><br>');
  }

  escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
  }
}
