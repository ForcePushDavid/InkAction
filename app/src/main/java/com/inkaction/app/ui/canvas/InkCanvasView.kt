package com.inkaction.app.ui.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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
    var currentSize: Float = 6f

    var templateType: Int = 0
    fun setTemplate(type: Int) {
        templateType = type
        invalidate()
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

    init {
        // Required for eraser PorterDuff CLEAR mode
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val toolType = event.getToolType(0)
        
        // Strict Palm Rejection: ONLY allow S-Pen (Stylus/Eraser). Ignore all fingers/palms.
        if (toolType != MotionEvent.TOOL_TYPE_STYLUS && toolType != MotionEvent.TOOL_TYPE_ERASER) {
            return false
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
        
        val eraserRadius = 30f
        
        fun eraseIntersections(touchX: Float, touchY: Float): Boolean {
            var erased = false
            val toRemove = strokes.filter { stroke ->
                stroke.points.any { p ->
                    Math.hypot((p.x - touchX).toDouble(), (p.y - touchY).toDouble()) < eraserRadius
                }
            }
            if (toRemove.isNotEmpty()) {
                strokes.removeAll(toRemove.toSet())
                toRemove.forEach { undoStack.push(it) }
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
                    stroke.points.add(InkPoint(x, y, pressure))
                    stroke.buildPath()
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
                            stroke.points.add(
                                InkPoint(
                                    event.getHistoricalX(h),
                                    event.getHistoricalY(h),
                                    event.getHistoricalPressure(h)
                                )
                            )
                        }
                        stroke.points.add(InkPoint(x, y, pressure))
                        stroke.buildPath()
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
                        stroke.points.add(InkPoint(x, y, pressure))
                        stroke.buildPath()
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

        templatePaint.color = if (isDarkMode) Color.parseColor("#30363D") else Color.parseColor("#D0D7DE")

        if (templateType > 0) {
            when (templateType) {
                1 -> { // Dots
                    val spacing = 50f
                    for (x in 0..width step spacing.toInt()) {
                        for (y in 0..height step spacing.toInt()) {
                            canvas.drawCircle(x.toFloat(), y.toFloat(), 2f, templatePaint)
                        }
                    }
                }
                2 -> { // Lines
                    val spacing = 80f
                    templatePaint.style = Paint.Style.STROKE
                    for (y in 0..height step spacing.toInt()) {
                        canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), templatePaint)
                    }
                    templatePaint.style = Paint.Style.FILL
                }
                3 -> { // Grid
                    val spacing = 80f
                    templatePaint.style = Paint.Style.STROKE
                    for (x in 0..width step spacing.toInt()) {
                        canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), templatePaint)
                    }
                    for (y in 0..height step spacing.toInt()) {
                        canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), templatePaint)
                    }
                    templatePaint.style = Paint.Style.FILL
                }
            }
        }

        for (stroke in strokes) {
            drawStroke(canvas, stroke)
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
            val minY = stroke.points.minOf { it.y }
            val maxY = stroke.points.maxOf { it.y }
            stroke to Pair(minY, maxY)
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
