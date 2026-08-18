/**
 * InkAction Canvas Engine
 * High-performance pointer events canvas with pressure sensitivity,
 * stroke smoothing, palm rejection, and undo/redo history.
 */
export class InkCanvas {
  constructor(canvasElement, options = {}) {
    this.canvas = canvasElement;
    this.ctx = this.canvas.getContext('2d');
    this.options = {
      penColor: '#f0f6fc',
      penSize: 3,
      tool: 'pen', // 'pen' | 'highlighter' | 'eraser'
      smoothing: true,
      pressureEnabled: true,
      onStrokeEnd: options.onStrokeEnd || (() => {}),
      onStrokeStart: options.onStrokeStart || (() => {}),
      ...options
    };

    this.isDrawing = false;
    this.currentStroke = [];
    this.strokes = [];
    this.undoStack = [];
    this.activePointerId = null;

    this.init();
  }

  init() {
    this.resize();
    window.addEventListener('resize', () => this.resize());

    // Pointer events with passive: false for touch cancellation
    this.canvas.addEventListener('pointerdown', (e) => this.handlePointerDown(e), { passive: false });
    this.canvas.addEventListener('pointermove', (e) => this.handlePointerMove(e), { passive: false });
    this.canvas.addEventListener('pointerup', (e) => this.handlePointerUp(e), { passive: false });
    this.canvas.addEventListener('pointercancel', (e) => this.handlePointerCancel(e), { passive: false });
    this.canvas.addEventListener('pointerleave', (e) => this.handlePointerUp(e), { passive: false });

    // Prevent default touch gestures (pinch-zoom, scrolling) on canvas
    this.canvas.addEventListener('touchstart', (e) => e.preventDefault(), { passive: false });
    this.canvas.addEventListener('touchmove', (e) => e.preventDefault(), { passive: false });
  }

  resize() {
    const rect = this.canvas.parentElement.getBoundingClientRect();
    const dpr = window.devicePixelRatio || 1;
    
    // Save current content before resize
    const prevWidth = this.canvas.width;
    const prevHeight = this.canvas.height;
    
    this.canvas.width = rect.width * dpr;
    this.canvas.height = rect.height * dpr;
    this.canvas.style.width = `${rect.width}px`;
    this.canvas.style.height = `${rect.height}px`;

    this.ctx.scale(dpr, dpr);
    this.redrawAll();
  }

  getPointerPos(e) {
    const rect = this.canvas.getBoundingClientRect();
    return {
      x: e.clientX - rect.left,
      y: e.clientY - rect.top,
      pressure: (e.pressure !== undefined && e.pressure > 0) ? e.pressure : 0.5,
      pointerType: e.pointerType,
      time: Date.now()
    };
  }

  handlePointerDown(e) {
    e.preventDefault();
    if (this.isDrawing) return;

    // Palm rejection: If pen is available, ignore generic touch fingers
    if (e.pointerType === 'touch' && this.hasActiveStylus) {
      return;
    }
    if (e.pointerType === 'pen') {
      this.hasActiveStylus = true;
    }

    this.isDrawing = true;
    this.activePointerId = e.pointerId;
    this.canvas.setPointerCapture(e.pointerId);

    const pt = this.getPointerPos(e);
    this.currentStroke = {
      tool: this.options.tool,
      color: this.options.penColor,
      size: this.options.penSize,
      points: [pt]
    };

    this.options.onStrokeStart();
    this.drawPoint(pt);
  }

  handlePointerMove(e) {
    if (!this.isDrawing || e.pointerId !== this.activePointerId) return;
    e.preventDefault();

    const pt = this.getPointerPos(e);
    this.currentStroke.points.push(pt);

    if (this.currentStroke.points.length > 2) {
      this.drawSmoothSegment(this.currentStroke);
    } else {
      this.drawPoint(pt);
    }
  }

  handlePointerUp(e) {
    if (!this.isDrawing || (e.pointerId && e.pointerId !== this.activePointerId)) return;
    this.isDrawing = false;
    
    try {
      if (this.canvas.hasPointerCapture(this.activePointerId)) {
        this.canvas.releasePointerCapture(this.activePointerId);
      }
    } catch (_) {}
    this.activePointerId = null;

    if (this.currentStroke && this.currentStroke.points.length > 0) {
      this.strokes.push(this.currentStroke);
      this.undoStack = []; // Clear redo stack on new action
      this.options.onStrokeEnd(this.strokes.length);
    }
    this.currentStroke = null;
  }

  handlePointerCancel(e) {
    this.handlePointerUp(e);
  }

  drawPoint(pt) {
    const ctx = this.ctx;
    ctx.save();
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';

    if (this.options.tool === 'eraser') {
      ctx.globalCompositeOperation = 'destination-out';
      ctx.beginPath();
      ctx.arc(pt.x, pt.y, this.options.penSize * 4, 0, Math.PI * 2);
      ctx.fill();
    } else if (this.options.tool === 'highlighter') {
      ctx.globalCompositeOperation = 'source-over';
      ctx.strokeStyle = this.options.penColor + '44'; // semi-transparent
      ctx.lineWidth = this.options.penSize * 4;
      ctx.beginPath();
      ctx.arc(pt.x, pt.y, (this.options.penSize * 4) / 2, 0, Math.PI * 2);
      ctx.fillStyle = ctx.strokeStyle;
      ctx.fill();
    } else {
      ctx.globalCompositeOperation = 'source-over';
      ctx.strokeStyle = this.options.penColor;
      const width = this.calculateLineWidth(this.options.penSize, pt.pressure);
      ctx.beginPath();
      ctx.arc(pt.x, pt.y, width / 2, 0, Math.PI * 2);
      ctx.fillStyle = this.options.penColor;
      ctx.fill();
    }
    ctx.restore();
  }

  drawSmoothSegment(stroke) {
    const pts = stroke.points;
    const len = pts.length;
    if (len < 2) return;

    const p0 = pts[len - 2];
    const p1 = pts[len - 1];
    const midX = (p0.x + p1.x) / 2;
    const midY = (p0.y + p1.y) / 2;

    const ctx = this.ctx;
    ctx.save();
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';

    if (stroke.tool === 'eraser') {
      ctx.globalCompositeOperation = 'destination-out';
      ctx.lineWidth = stroke.size * 4;
      ctx.beginPath();
      ctx.moveTo(p0.x, p0.y);
      ctx.lineTo(p1.x, p1.y);
      ctx.stroke();
    } else if (stroke.tool === 'highlighter') {
      ctx.globalCompositeOperation = 'source-over';
      ctx.strokeStyle = stroke.color + '44';
      ctx.lineWidth = stroke.size * 4;
      ctx.beginPath();
      ctx.moveTo(p0.x, p0.y);
      ctx.lineTo(p1.x, p1.y);
      ctx.stroke();
    } else {
      ctx.globalCompositeOperation = 'source-over';
      ctx.strokeStyle = stroke.color;
      ctx.lineWidth = this.calculateLineWidth(stroke.size, p1.pressure);
      ctx.beginPath();
      ctx.moveTo(p0.x, p0.y);
      ctx.quadraticCurveTo(p0.x, p0.y, midX, midY);
      ctx.stroke();
    }
    ctx.restore();
  }

  calculateLineWidth(baseSize, pressure) {
    if (!this.options.pressureEnabled || pressure === undefined) return baseSize;
    // Map pressure from [0, 1] to [0.5 * baseSize, 1.8 * baseSize]
    return baseSize * (0.4 + pressure * 1.2);
  }

  redrawAll() {
    const dpr = window.devicePixelRatio || 1;
    this.ctx.clearRect(0, 0, this.canvas.width / dpr, this.canvas.height / dpr);

    for (const stroke of this.strokes) {
      if (!stroke.points || stroke.points.length === 0) continue;
      const pts = stroke.points;
      
      this.ctx.save();
      this.ctx.lineCap = 'round';
      this.ctx.lineJoin = 'round';

      if (stroke.tool === 'eraser') {
        this.ctx.globalCompositeOperation = 'destination-out';
        this.ctx.lineWidth = stroke.size * 4;
      } else if (stroke.tool === 'highlighter') {
        this.ctx.globalCompositeOperation = 'source-over';
        this.ctx.strokeStyle = stroke.color + '44';
        this.ctx.lineWidth = stroke.size * 4;
      } else {
        this.ctx.globalCompositeOperation = 'source-over';
        this.ctx.strokeStyle = stroke.color;
      }

      this.ctx.beginPath();
      this.ctx.moveTo(pts[0].x, pts[0].y);

      for (let i = 1; i < pts.length; i++) {
        const p0 = pts[i - 1];
        const p1 = pts[i];
        const midX = (p0.x + p1.x) / 2;
        const midY = (p0.y + p1.y) / 2;
        if (stroke.tool !== 'eraser' && stroke.tool !== 'highlighter') {
          this.ctx.lineWidth = this.calculateLineWidth(stroke.size, p1.pressure);
        }
        this.ctx.quadraticCurveTo(p0.x, p0.y, midX, midY);
      }
      this.ctx.stroke();
      this.ctx.restore();
    }
  }

  undo() {
    if (this.strokes.length === 0) return;
    const removed = this.strokes.pop();
    this.undoStack.push(removed);
    this.redrawAll();
    this.options.onStrokeEnd(this.strokes.length);
  }

  redo() {
    if (this.undoStack.length === 0) return;
    const restored = this.undoStack.pop();
    this.strokes.push(restored);
    this.redrawAll();
    this.options.onStrokeEnd(this.strokes.length);
  }

  clear() {
    if (this.strokes.length === 0) return;
    this.undoStack.push([...this.strokes]);
    this.strokes = [];
    this.redrawAll();
    this.options.onStrokeEnd(0);
  }

  setTool(tool) {
    this.options.tool = tool;
  }

  setColor(color) {
    this.options.penColor = color;
  }

  setSize(size) {
    this.options.penSize = size;
  }

  isEmpty() {
    return this.strokes.length === 0;
  }

  /**
   * Generates high-quality snapshot of the drawing for Google Gemini Multimodal API.
   * Renders strokes onto a clean white/contrasting background for optimal OCR & multimodal recognition.
   */
  getSnapshotBase64() {
    if (this.isEmpty()) return null;

    const tempCanvas = document.createElement('canvas');
    tempCanvas.width = this.canvas.width;
    tempCanvas.height = this.canvas.height;
    const tempCtx = tempCanvas.getContext('2d');

    // Fill background with white for maximum OCR/AI clarity
    tempCtx.fillStyle = '#ffffff';
    tempCtx.fillRect(0, 0, tempCanvas.width, tempCanvas.height);

    const dpr = window.devicePixelRatio || 1;
    tempCtx.scale(dpr, dpr);

    // Draw all strokes in high contrast dark color if drawn with light theme or dark theme
    for (const stroke of this.strokes) {
      if (!stroke.points || stroke.points.length === 0) continue;
      const pts = stroke.points;
      
      tempCtx.save();
      tempCtx.lineCap = 'round';
      tempCtx.lineJoin = 'round';

      if (stroke.tool === 'eraser') {
        tempCtx.fillStyle = '#ffffff';
        tempCtx.strokeStyle = '#ffffff';
        tempCtx.lineWidth = stroke.size * 4;
      } else if (stroke.tool === 'highlighter') {
        tempCtx.strokeStyle = 'rgba(255, 220, 0, 0.4)';
        tempCtx.lineWidth = stroke.size * 4;
      } else {
        // High contrast black/dark blue for OCR clarity
        tempCtx.strokeStyle = (stroke.color === '#ffffff' || stroke.color === '#f0f6fc') ? '#111827' : stroke.color;
      }

      tempCtx.beginPath();
      tempCtx.moveTo(pts[0].x, pts[0].y);

      for (let i = 1; i < pts.length; i++) {
        const p0 = pts[i - 1];
        const p1 = pts[i];
        const midX = (p0.x + p1.x) / 2;
        const midY = (p0.y + p1.y) / 2;
        if (stroke.tool !== 'eraser' && stroke.tool !== 'highlighter') {
          tempCtx.lineWidth = Math.max(2, this.calculateLineWidth(stroke.size, p1.pressure));
        }
        tempCtx.quadraticCurveTo(p0.x, p0.y, midX, midY);
      }
      tempCtx.stroke();
      tempCtx.restore();
    }

    return tempCanvas.toDataURL('image/png').split(',')[1];
  }
}
