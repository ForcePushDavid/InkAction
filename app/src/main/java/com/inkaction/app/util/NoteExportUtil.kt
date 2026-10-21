package com.inkaction.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.FileProvider
import com.inkaction.app.R
import com.inkaction.app.data.model.SavedNote
import com.inkaction.app.ui.canvas.InkStroke
import com.inkaction.app.ui.canvas.ToolType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object NoteExportUtil {

    /**
     * Renders a list of InkStrokes into a high-resolution Bitmap with paper background.
     */
    fun renderStrokesToBitmap(strokes: List<InkStroke>): Bitmap? {
        if (strokes.isEmpty()) return null

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        var hasValidPoints = false
        for (stroke in strokes) {
            if (stroke.tool == ToolType.ERASER) continue
            for (point in stroke.points) {
                hasValidPoints = true
                if (point.x < minX) minX = point.x
                if (point.y < minY) minY = point.y
                if (point.x > maxX) maxX = point.x
                if (point.y > maxY) maxY = point.y
            }
        }

        if (!hasValidPoints || minX >= maxX || minY >= maxY) return null

        val padding = 40f
        val width = (maxX - minX + padding * 2).toInt().coerceIn(400, 4000)
        val height = (maxY - minY + padding * 2).toInt().coerceIn(400, 10000)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        canvas.translate(-minX + padding, -minY + padding)

        val paint = Paint().apply {
            isAntiAlias = true
            isDither = true
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }

        for (stroke in strokes) {
            if (stroke.tool == ToolType.ERASER) continue
            paint.color = stroke.color
            // On white paper background, draw light/white ink strokes in crisp dark color
            if (stroke.color == Color.WHITE || stroke.color == Color.parseColor("#F0F6FC")) {
                paint.color = Color.BLACK
            }
            paint.strokeWidth = stroke.baseWidth
            if (stroke.tool == ToolType.HIGHLIGHTER) {
                paint.alpha = 90
                paint.strokeWidth = stroke.baseWidth * 2.5f
            } else {
                paint.alpha = 255
            }

            stroke.buildPath()
            canvas.drawPath(stroke.path, paint)
        }

        return bitmap
    }

    /**
     * Exports and shares the handwritten S-Pen ink drawing of the note as a PNG image.
     */
    suspend fun shareHandwrittenNote(context: Context, note: SavedNote): Boolean = withContext(Dispatchers.IO) {
        val strokesFile = File(context.filesDir, "strokes_${note.id}.json")
        if (!strokesFile.exists()) {
            return@withContext false
        }
        val strokeListType = object : TypeToken<List<InkStroke>>() {}.type
        val strokes: List<InkStroke> = try {
            Gson().fromJson(strokesFile.readText(), strokeListType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        if (strokes.isEmpty()) return@withContext false

        val bitmap = renderStrokesToBitmap(strokes) ?: return@withContext false
        val imageFile = File(context.cacheDir, "handwritten_note_${note.id}.png")
        if (imageFile.exists()) imageFile.delete()

        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, note.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        withContext(Dispatchers.Main) {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_handwritten_note)))
        }
        true
    }

    /**
     * Shares the synthesized note as clean text / Markdown.
     */
    fun shareTextNote(context: Context, note: SavedNote) {
        val text = buildString {
            append("# ").append(note.title.ifBlank { "Poznámka" }).append("\n\n")
            if (note.summary.isNotBlank()) {
                append(note.summary).append("\n\n")
            }
            append(note.markdown)
            if (!note.aiEnhancement.isNullOrBlank()) {
                append("\n\n---\n### AI Insight\n").append(note.aiEnhancement)
            }
        }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, note.title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.share_text_note)))
    }
}
