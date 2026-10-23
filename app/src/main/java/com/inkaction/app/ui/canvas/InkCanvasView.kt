package com.inkaction.app.ui.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.Stack

class InkCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    val strokes = mutableListOf<InkStroke>()
    private val undoStack = Stack<InkStroke>()
    private var currentStroke: InkStroke? = null

    fun loadStrokes(newStrokes: List<InkStroke>) {
        strokes.clear()
        strokes.addAll(newStrokes)
        strokes.forEach { it.buildPath() }
        undoStack.clear()
        invalidate()
    }

    var currentTool: ToolType = ToolType.PEN
    var currentColor: Int = Color.parseColor("#F0F6FC")
    var currentSize: Float = 3.5f

    var templateType: Int = 0
    fun setTemplate(type: Int) {
        if (templateType != type) {
            templateType = type
            invalidate()
        }
    }

    var isDarkMode: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var onStrokeStarted: (() -> Unit)? = null
    var onStrokeFinished: ((Int) -> Unit)? = null
    var onFingerTouchBlocked: (() -> Unit)? = null

    var isFingerDrawingEnabled: Boolean = true

    private var hasActiveStylus = false

    private val strokePaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val eraserPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val clipRect = Rect()
    private val clipRectF = RectF()
    private val toRemoveStrokes = ArrayList<InkStroke>()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val toolType = event.getToolType(0)
        
        // Strict Palm Rejection: ONLY allow S-Pen (Stylus/Eraser) unless finger drawing is explicitly enabled.
        if (toolType != MotionEvent.TOOL_TYPE_STYLUS && toolType != MotionEvent.TOOL_TYPE_ERASER) {
            if (!isFingerDrawingEnabled) {
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    onFingerTouchBlocked?.invoke()
                }
                return false
            }
        }

        val x = event.x
        val y = event.y
        val pressure = if (event.pressure > 0f) event.pressure else 0.5f

        // Check if pen barrel button is pressed -> automatically switch to eraser
        val effectiveTool = if (toolType == MotionEvent.TOOL_TYPE_ERASER || 
            (event.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0) {
            ToolType.ERASER
        } else {
            currentTool
        }
        
        val eraserRadius = 35f
        val eraserRadiusSq = eraserRadius * eraserRadius
        
        fun eraseIntersections(touchX: Float, touchY: Float): Boolean {
            var erased = false
            toRemoveStrokes.clear()

            val eLeft = touchX - eraserRadius
            val eTop = touchY - eraserRadius
            val eRight = touchX + eraserRadius
            val eBottom = touchY + eraserRadius

            for (i in 0 until strokes.size) {
                val stroke = strokes[i]
                val b = stroke.bounds
                if (b.right < eLeft || b.left > eRight || b.bottom < eTop || b.top > eBottom) {
                    continue
                }
                val pts = stroke.points
                for (j in 0 until pts.size) {
                    val p = pts[j]
                    val dx = p.x - touchX
                    val dy = p.y - touchY
                    if (dx * dx + dy * dy <= eraserRadiusSq) {
                        toRemoveStrokes.add(stroke)
                        break
                    }
                }
            }

            if (toRemoveStrokes.isNotEmpty()) {
                strokes.removeAll(toRemoveStrokes)
                for (k in 0 until toRemoveStrokes.size) {
                    undoStack.push(toRemoveStrokes[k])
                }
                erased = true
            }
            return erased
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                onStrokeStarted?.invoke()
                if (effectiveTool == ToolType.ERASER) {
                    if (eraseIntersections(x, y)) invalidate()
                } else {
                    val stroke = InkStroke(
                        tool = effectiveTool,
                        color = currentColor,
                        baseWidth = currentSize
                    )
                    stroke.addPointIncremental(InkPoint(x, y, pressure))
                    currentStroke = stroke
                    strokes.add(stroke)
                    undoStack.clear()
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (effectiveTool == ToolType.ERASER) {
                    val historySize = event.historySize
                    var erased = false
                    for (h in 0 until historySize) {
                        if (eraseIntersections(event.getHistoricalX(h), event.getHistoricalY(h))) erased = true
                    }
                    if (eraseIntersections(x, y)) erased = true
                    if (erased) invalidate()
                } else {
                    currentStroke?.let { stroke ->
                        val historySize = event.historySize
                        for (h in 0 until historySize) {
                            stroke.addPointIncremental(
                                InkPoint(
                                    event.getHistoricalX(h),
                                    event.getHistoricalY(h),
                                    event.getHistoricalPressure(h)
                                )
                            )
                        }
                        stroke.addPointIncremental(InkPoint(x, y, pressure))
                        invalidate()
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                if (effectiveTool == ToolType.ERASER) {
                    onStrokeFinished?.invoke(strokes.size)
                } else {
                    currentStroke?.let { stroke ->
                        stroke.addPointIncremental(InkPoint(x, y, pressure))
                        currentStroke = null
                        invalidate()
                        onStrokeFinished?.invoke(strokes.size)
                    }
                }
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    private val templatePaint = Paint().apply {
        color = Color.parseColor("#30363D")
        strokeWidth = 3f
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!canvas.getClipBounds(clipRect)) {
            clipRect.set(0, 0, width, height)
        }
        clipRectF.set(clipRect)

        templatePaint.color = if (isDarkMode) Color.parseColor("#30363D") else Color.parseColor("#D0D7DE")

        if (templateType > 0) {
            when (templateType) {
                1 -> { // Dots - viewport frustum culling
                    val spacing = 50f
                    val startX = ((clipRect.left / spacing).toInt() * spacing.toInt()).coerceAtLeast(0)
                    val endX = clipRect.right.coerceAtMost(width)
                    val startY = ((clipRect.top / spacing).toInt() * spacing.toInt()).coerceAtLeast(0)
                    val endY = clipRect.bottom.coerceAtMost(height)

                    var cx = startX
                    while (cx <= endX) {
                        var cy = startY
                        while (cy <= endY) {
                            canvas.drawCircle(cx.toFloat(), cy.toFloat(), 2f, templatePaint)
                            cy += spacing.toInt()
                        }
                        cx += spacing.toInt()
                    }
                }
                2 -> { // Lines - viewport frustum culling
                    val spacing = 80f
                    templatePaint.style = Paint.Style.STROKE
                    val startY = ((clipRect.top / spacing).toInt() * spacing.toInt()).coerceAtLeast(0)
                    val endY = clipRect.bottom.coerceAtMost(height)

                    var cy = startY
                    while (cy <= endY) {
                        canvas.drawLine(0f, cy.toFloat(), width.toFloat(), cy.toFloat(), templatePaint)
                        cy += spacing.toInt()
                    }
                    templatePaint.style = Paint.Style.FILL
                }
                3 -> { // Grid - viewport frustum culling
                    val spacing = 80f
                    templatePaint.style = Paint.Style.STROKE
                    val startX = ((clipRect.left / spacing).toInt() * spacing.toInt()).coerceAtLeast(0)
                    val endX = clipRect.right.coerceAtMost(width)
                    val startY = ((clipRect.top / spacing).toInt() * spacing.toInt()).coerceAtLeast(0)
                    val endY = clipRect.bottom.coerceAtMost(height)

                    var cx = startX
                    while (cx <= endX) {
                        canvas.drawLine(cx.toFloat(), 0f, cx.toFloat(), height.toFloat(), templatePaint)
                        cx += spacing.toInt()
                    }
                    var cy = startY
                    while (cy <= endY) {
                        canvas.drawLine(0f, cy.toFloat(), width.toFloat(), cy.toFloat(), templatePaint)
                        cy += spacing.toInt()
                    }
                    templatePaint.style = Paint.Style.FILL
                }
            }
        }

        // Viewport frustum culling: only draw strokes that intersect the visible screen clip!
        for (i in 0 until strokes.size) {
            val stroke = strokes[i]
            if (RectF.intersects(stroke.bounds, clipRectF)) {
                drawStroke(canvas, stroke)
            }
        }
    }

    private fun drawStroke(canvas: Canvas, stroke: InkStroke) {
        val paint = if (stroke.tool == ToolType.ERASER) eraserPaint else strokePaint

        var drawColor = stroke.color
        if (!isDarkMode) {
            if (stroke.color == Color.WHITE || stroke.color == Color.parseColor("#F0F6FC")) {
                drawColor = Color.parseColor("#1F2328") // Dark text for light mode
            }
        } else {
            if (stroke.color == Color.BLACK || stroke.color == Color.parseColor("#1F2328")) {
                drawColor = Color.parseColor("#F0F6FC") // Light text for dark mode
            }
        }

        when (stroke.tool) {
            ToolType.PEN -> {
                paint.color = drawColor
                paint.alpha = 255
                paint.strokeWidth = stroke.baseWidth
            }
            ToolType.HIGHLIGHTER -> {
                paint.color = drawColor
                paint.alpha = 80 // Semi-transparent for highlighter
                paint.strokeWidth = stroke.baseWidth * 3.5f
            }
            ToolType.ERASER -> {
                paint.strokeWidth = stroke.baseWidth * 4f
            }
        }

        canvas.drawPath(stroke.path, paint)
    }

    fun undo() {
        if (strokes.isNotEmpty()) {
            val removed = strokes.removeAt(strokes.size - 1)
            undoStack.push(removed)
            invalidate()
            onStrokeFinished?.invoke(strokes.size)
        }
    }

    fun redo() {
        if (undoStack.isNotEmpty()) {
            val restored = undoStack.pop()
            strokes.add(restored)
            invalidate()
            onStrokeFinished?.invoke(strokes.size)
        }
    }

    fun clearCanvas() {
        strokes.clear()
        undoStack.clear()
        invalidate()
        onStrokeFinished?.invoke(0)
    }

    fun isEmpty(): Boolean = strokes.isEmpty()

    /**
     * Generates a list of scaled, high-contrast bitmaps (pages) to prevent OutOfMemory and allow infinite canvas.
     */
    fun createOcrBitmaps(): List<Bitmap> {
        if (width <= 0 || height <= 0 || isEmpty()) return emptyList()

        val strokeBounds = strokes.mapNotNull { stroke ->
            if (stroke.points.isEmpty()) return@mapNotNull null
            stroke to Pair(stroke.bounds.top, stroke.bounds.bottom)
        }

        if (strokeBounds.isEmpty()) return emptyList()

        val globalMinY = strokeBounds.minOf { it.second.first }
        val globalMaxY = strokeBounds.maxOf { it.second.second }

        val pageHeight = 2000f
        val bitmaps = mutableListOf<Bitmap>()

        var currentY = globalMinY - 20f // small padding top
        if (currentY < 0f) currentY = 0f

        while (currentY < globalMaxY) {
            var cutY = currentY + pageHeight
            
            if (cutY < globalMaxY) {
                var foundCut = false
                for (testY in cutY.toInt().. (cutY + 600).toInt() step 20) {
                    val isClean = strokeBounds.none { (_, bounds) ->
                        bounds.first < testY && bounds.second > testY
                    }
                    if (isClean) {
                        cutY = testY.toFloat()
                        foundCut = true
                        break
                    }
                }
                
                if (!foundCut) {
                    for (testY in cutY.toInt() downTo (cutY - 600).toInt() step 20) {
                        val isClean = strokeBounds.none { (_, bounds) ->
                            bounds.first < testY && bounds.second > testY
                        }
                        if (isClean) {
                            cutY = testY.toFloat()
                            foundCut = true
                            break
                        }
                    }
                }
            } else {
                cutY = globalMaxY + 20f // small padding bottom
            }
            
            val chunkHeight = (cutY - currentY).toInt().coerceAtLeast(100)
            val scale = 0.5f
            val scaledWidth = (width * scale).toInt()
            val scaledHeight = (chunkHeight * scale).toInt()
            
            val bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
            val ocrCanvas = Canvas(bitmap)
            ocrCanvas.drawColor(Color.WHITE)
            
            ocrCanvas.scale(scale, scale)
            ocrCanvas.translate(0f, -currentY)
            
            val ocrPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            }

            for (stroke in strokes) {
                val bounds = strokeBounds.find { it.first == stroke }?.second
                if (bounds != null && (bounds.second < currentY || bounds.first > cutY)) continue
                
                when (stroke.tool) {
                    ToolType.PEN -> {
                        val darkInk = if (stroke.color == Color.WHITE || stroke.color == Color.parseColor("#F0F6FC")) {
                            Color.parseColor("#111827")
                        } else {
                            stroke.color
                        }
                        ocrPaint.color = darkInk
                        ocrPaint.alpha = 255
                        ocrPaint.strokeWidth = stroke.baseWidth.coerceAtLeast(3f)
                        ocrCanvas.drawPath(stroke.path, ocrPaint)
                    }
                    ToolType.HIGHLIGHTER -> {
                        ocrPaint.color = Color.parseColor("#FFE500")
                        ocrPaint.alpha = 100
                        ocrPaint.strokeWidth = stroke.baseWidth * 3f
                        ocrCanvas.drawPath(stroke.path, ocrPaint)
                    }
                    ToolType.ERASER -> { }
                }
            }
            
            bitmaps.add(bitmap)
            currentY = cutY
        }
        
        return bitmaps
    }
}
