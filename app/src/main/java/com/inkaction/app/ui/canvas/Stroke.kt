package com.inkaction.app.ui.canvas

import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
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

    @Transient
    val bounds: RectF = RectF()

    fun buildPath() {
        path.reset()
        if (points.isEmpty()) {
            bounds.setEmpty()
            return
        }

        var minX = points[0].x
        var minY = points[0].y
        var maxX = points[0].x
        var maxY = points[0].y

        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            val p0 = points[i - 1]
            val p1 = points[i]
            val midX = (p0.x + p1.x) / 2f
            val midY = (p0.y + p1.y) / 2f
            path.quadTo(p0.x, p0.y, midX, midY)

            if (p1.x < minX) minX = p1.x
            if (p1.x > maxX) maxX = p1.x
            if (p1.y < minY) minY = p1.y
            if (p1.y > maxY) maxY = p1.y
        }

        val pad = (baseWidth * 4f).coerceAtLeast(12f)
        bounds.set(minX - pad, minY - pad, maxX + pad, maxY + pad)
    }

    /**
     * O(1) incremental point addition for butter-smooth active drawing.
     * Avoids resetting and rebuilding the entire path on every touch move.
     */
    fun addPointIncremental(point: InkPoint) {
        points.add(point)
        val pad = (baseWidth * 4f).coerceAtLeast(12f)
        if (points.size == 1) {
            path.reset()
            path.moveTo(point.x, point.y)
            bounds.set(point.x - pad, point.y - pad, point.x + pad, point.y + pad)
        } else {
            val p0 = points[points.size - 2]
            val midX = (p0.x + point.x) / 2f
            val midY = (p0.y + point.y) / 2f
            path.quadTo(p0.x, p0.y, midX, midY)
            bounds.union(point.x - pad, point.y - pad, point.x + pad, point.y + pad)
        }
    }
}
