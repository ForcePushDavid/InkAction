/**
 * Auto-Push Inactivity Engine
 * Automatically detects when the user has stopped writing and triggers AI processing,
 * with real-time visual countdown and manual trigger overrides.
 */
export class AutoPushEngine {
  constructor(options = {}) {
    this.delayMs = options.delayMs || 6000; // default 6s inactivity
    this.enabled = options.enabled !== undefined ? options.enabled : true;
    this.onTrigger = options.onTrigger || (() => {});
    this.onTick = options.onTick || (() => {});
    this.onStateChange = options.onStateChange || (() => {});

    this.timer = null;
    this.animationFrame = null;
    this.startTime = null;
    this.remainingMs = this.delayMs;
    this.isArmed = false;
    this.isProcessing = false;
  }

  setDelay(ms) {
    this.delayMs = ms;
    if (this.isArmed) {
      this.restart();
    }
  }

  setEnabled(enabled) {
    this.enabled = enabled;
    if (!enabled) {
      this.cancel();
    }
  }

  /**
   * Called when a stroke starts (drawing in progress).
   * Freezes countdown and disarms.
   */
  onStrokeStart() {
    this.cancel();
  }

  /**
   * Called when a stroke finishes.
   * Arms countdown if canvas has strokes and auto-push is enabled.
   */
  onStrokeEnd(strokeCount) {
    if (strokeCount === 0 || !this.enabled || this.isProcessing) {
      this.cancel();
      return;
    }

    this.startCountdown();
  }

  startCountdown() {
    this.cancel();
    this.isArmed = true;
    this.startTime = Date.now();
    this.onStateChange({ armed: true, remainingSec: Math.ceil(this.delayMs / 1000) });

    const updateTick = () => {
      if (!this.isArmed) return;

      const elapsed = Date.now() - this.startTime;
      const progress = Math.min(1, elapsed / this.delayMs);
      const remainingSec = Math.max(0, Math.ceil((this.delayMs - elapsed) / 1000));

      this.onTick({ progress, remainingSec });

      if (elapsed >= this.delayMs) {
        this.trigger();
      } else {
        this.animationFrame = requestAnimationFrame(updateTick);
      }
    };

    this.animationFrame = requestAnimationFrame(updateTick);
  }

  trigger() {
    this.cancel();
    if (this.isProcessing) return;

    this.isProcessing = true;
    this.onStateChange({ armed: false, processing: true });
    
    // Provide slight haptic feedback if supported (e.g. Galaxy S26 / Tab S9)
    if (navigator.vibrate) {
      navigator.vibrate(40);
    }

    Promise.resolve(this.onTrigger()).finally(() => {
      this.isProcessing = false;
      this.onStateChange({ armed: false, processing: false });
    });
  }

  cancel() {
    this.isArmed = false;
    if (this.animationFrame) {
      cancelAnimationFrame(this.animationFrame);
      this.animationFrame = null;
    }
    this.onTick({ progress: 0, remainingSec: 0 });
    this.onStateChange({ armed: false, processing: this.isProcessing });
  }

  restart() {
    this.cancel();
    this.startCountdown();
  }
}
