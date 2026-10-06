package com.inkaction.app.worker

import android.content.Context
import android.graphics.Bitmap
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.io.File
import java.io.FileOutputStream

object OfflineQueue {
    fun enqueue(
        context: Context,
        bitmaps: List<Bitmap>,
        apiKey: String,
        modelName: String,
        noteLanguage: String,
        defaultEventTime: String,
        existingTodosStr: String,
        existingEventsStr: String,
        generateNote: Boolean,
        generateTodos: Boolean
    ) {
        try {
            val queueDir = File(context.filesDir, "offline_queue")
            if (!queueDir.exists()) queueDir.mkdirs()
            
            val taskId = System.currentTimeMillis().toString()
            val imagePaths = bitmaps.mapIndexed { index, bitmap ->
                val file = File(queueDir, "img_${taskId}_${index}.png")
                val out = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
                out.close()
                file.absolutePath
            }.toTypedArray()

            val workData = Data.Builder()
                .putString("api_key", apiKey)
                .putString("model_name", modelName)
                .putString("language", noteLanguage)
                .putString("default_event_time", defaultEventTime)
                .putString("existing_todos", existingTodosStr)
                .putString("existing_events", existingEventsStr)
                .putBoolean("generate_note", generateNote)
                .putBoolean("generate_todos", generateTodos)
                .putStringArray("image_paths", imagePaths)
                .build()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<GeminiOfflineWorker>()
                .setConstraints(constraints)
                .setInputData(workData)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
