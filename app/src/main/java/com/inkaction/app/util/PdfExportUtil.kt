package com.inkaction.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object PdfExportUtil {
    fun exportNoteToPdf(context: Context, title: String, content: String) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val textPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 14f
            isAntiAlias = true
        }
        val titlePaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 24f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        var yPos = 50f
        canvas.drawText(title, 50f, yPos, titlePaint)
        yPos += 40f

        val textLayout = StaticLayout.Builder.obtain(content, 0, content.length, textPaint, 495)
            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(1f, 1f)
            .setIncludePad(false)
            .build()

        canvas.save()
        canvas.translate(50f, yPos)
        textLayout.draw(canvas)
        canvas.restore()

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "ExportedNote.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(Intent.createChooser(intent, "Open PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }
    }
}
