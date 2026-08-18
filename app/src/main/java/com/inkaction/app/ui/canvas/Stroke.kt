package com.inkaction.app.ui.canvas

import android.graphics.Color
import android.graphics.Path
import kotlin.jvm.Transient

enum class ToolType {
    PEN,
    HIGHLIGHTER,
    ERASER
}

data class InkPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 0.5f,
    val timestamp: Long = System.currentTimeMillis()
)

data class InkStroke(
    val points: MutableList<InkPoint> = mutableListOf(),
    val tool: ToolType = ToolType.PEN,
    val color: Int = Color.parseColor("#F0F6FC"),
    val baseWidth: Float = 6f
) {
    @Transient
    val path: Path = Path()

    fun buildPath() {
        path.reset()
        if (points.isEmpty()) return
        
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            val p0 = points[i - 1]
            val p1 = points[i]
            val midX = (p0.x + p1.x) / 2
            val midY = (p0.y + p1.y) / 2
            path.quadTo(p0.x, p0.y, midX, midY)
        }
    }
}
