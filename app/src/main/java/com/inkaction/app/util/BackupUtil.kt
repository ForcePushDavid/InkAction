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
