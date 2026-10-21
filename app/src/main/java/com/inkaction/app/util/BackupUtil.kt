package com.inkaction.app.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupUtil {

    private val BACKUP_FILES = listOf(
        "saved_notes.json",
        "saved_todos.json",
        "saved_folders.json"
    )

    fun exportToZip(context: Context, filesDir: File): Uri? {
        try {
            val backupFile = File(context.cacheDir, "inkaction_backup.zip")
            if (backupFile.exists()) backupFile.delete()

            val fos = FileOutputStream(backupFile)
            val zos = ZipOutputStream(fos)

            BACKUP_FILES.forEach { fileName ->
                val file = File(filesDir, fileName)
                if (file.exists()) {
                    val entry = ZipEntry(fileName)
                    zos.putNextEntry(entry)
                    val fis = FileInputStream(file)
                    fis.copyTo(zos)
                    fis.close()
                    zos.closeEntry()
                }
            }
            zos.close()
            fos.close()

            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Exports all notes as individual human-readable Markdown (.md) files in a ZIP archive.
     */
    fun exportNotesToMarkdownZip(context: Context, notes: List<com.inkaction.app.data.model.SavedNote>): Uri? {
        try {
            val zipFile = File(context.cacheDir, "inkaction_notes_markdown.zip")
            if (zipFile.exists()) zipFile.delete()

            val fos = FileOutputStream(zipFile)
            val zos = ZipOutputStream(fos)
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())

            notes.forEachIndexed { index, note ->
                val safeTitle = note.title
                    .replace("[\\\\/:*?\"<>|]".toRegex(), "_")
                    .trim()
                    .ifBlank { "Note_${index + 1}" }
                val fileName = "${index + 1}_$safeTitle.md"

                val contentBuilder = StringBuilder()
                contentBuilder.append("# ").append(note.title.ifBlank { "Poznámka" }).append("\n\n")
                contentBuilder.append("**Datum:** ").append(sdf.format(java.util.Date(note.timestamp))).append("\n")
                if (note.tags.isNotEmpty()) {
                    contentBuilder.append("**Štítky:** ").append(note.tags.joinToString(", ") { "#$it" }).append("\n")
                }
                contentBuilder.append("\n")
                if (note.summary.isNotBlank()) {
                    contentBuilder.append("> ").append(note.summary.replace("\n", "\n> ")).append("\n\n")
                }
                contentBuilder.append(note.markdown).append("\n")

                if (!note.aiEnhancement.isNullOrBlank()) {
                    contentBuilder.append("\n---\n### AI Insight\n").append(note.aiEnhancement).append("\n")
                }

                val entry = ZipEntry(fileName)
                zos.putNextEntry(entry)
                zos.write(contentBuilder.toString().toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            zos.close()
            fos.close()

            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                zipFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun importFromZip(context: Context, zipUri: Uri, filesDir: File): Boolean {
        try {
            val inputStream = context.contentResolver.openInputStream(zipUri) ?: return false
            val zis = ZipInputStream(inputStream)

            var entry = zis.nextEntry
            while (entry != null) {
                if (BACKUP_FILES.contains(entry.name)) {
                    val outFile = File(filesDir, entry.name)
                    val fos = FileOutputStream(outFile)
                    zis.copyTo(fos)
                    fos.close()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
            zis.close()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
