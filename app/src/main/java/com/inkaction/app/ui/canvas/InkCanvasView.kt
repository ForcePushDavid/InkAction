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

    private val strokes = mutableListOf<InkStroke>()
    private val undoStack = Stack<InkStroke>()
    private var currentStroke: InkStroke? = null

    var currentTool: ToolType = ToolType.PEN
    var currentColor: Int = Color.parseColor("#F0F6FC")
    var currentSize: Float = 6f

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

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                onStrokeStarted?.invoke()
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
                return true
            }

            MotionEvent.ACTION_MOVE -> {
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
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                currentStroke?.let { stroke ->
                    stroke.points.add(InkPoint(x, y, pressure))
                    stroke.buildPath()
                    currentStroke = null
                    invalidate()
                    onStrokeFinished?.invoke(strokes.size)
                }
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (stroke in strokes) {
            drawStroke(canvas, stroke)
        }
    }

    private fun drawStroke(canvas: Canvas, stroke: InkStroke) {
        val paint = if (stroke.tool == ToolType.ERASER) eraserPaint else strokePaint

        when (stroke.tool) {
            ToolType.PEN -> {
                paint.color = stroke.color
                paint.alpha = 255
                paint.strokeWidth = stroke.baseWidth
            }
            ToolType.HIGHLIGHTER -> {
                paint.color = stroke.color
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
     * Generates a high-contrast bitmap on a pure white background for Google Gemini Multimodal API.
     */
    fun createOcrBitmap(): Bitmap? {
        if (width <= 0 || height <= 0 || isEmpty()) return null

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val ocrCanvas = Canvas(bitmap)

        // Pure white background for maximum Gemini OCR & Vision clarity
        ocrCanvas.drawColor(Color.WHITE)

        val ocrPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }

        for (stroke in strokes) {
            when (stroke.tool) {
                ToolType.PEN -> {
                    // Convert light strokes to dark ink for Gemini OCR
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
                ToolType.ERASER -> {
                    // Skip or erase on OCR
                }
            }
        }

        return bitmap
    }
}
