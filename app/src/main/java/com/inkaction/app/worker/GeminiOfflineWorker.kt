package com.inkaction.app.worker

import android.content.Context
import android.graphics.BitmapFactory
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.inkaction.app.InkActionApp
import com.inkaction.app.ai.GeminiAgentEngine
import com.inkaction.app.ai.AgentPipelineStatus
import com.inkaction.app.data.model.SavedTodo
import java.io.File
import java.util.UUID

class GeminiOfflineWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val apiKey = inputData.getString("api_key") ?: return Result.failure()
        val modelName = inputData.getString("model_name") ?: "gemini-3.8-flash"
        val language = inputData.getString("language") ?: "Auto-detect"
        val defaultEventTime = inputData.getString("default_event_time") ?: "08:00"
        val existingTodosStr = inputData.getString("existing_todos") ?: ""
        val existingEventsStr = inputData.getString("existing_events") ?: ""
        val generateNote = inputData.getBoolean("generate_note", true)
        val generateTodos = inputData.getBoolean("generate_todos", true)
        val imagePaths = inputData.getStringArray("image_paths") ?: emptyArray()
        
        if (apiKey.isBlank()) return Result.failure()

        val app = context.applicationContext as InkActionApp
        val noteRepository = app.noteRepository
        val engine = GeminiAgentEngine(apiKey, modelName)

        val bitmaps = imagePaths.mapNotNull { path ->
            val file = File(path)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        }

        if (bitmaps.isEmpty()) {
            return Result.failure()
        }

        return try {
            var finalStatus: AgentPipelineStatus? = null
            engine.processInkBitmap(
                bitmaps = bitmaps,
                language = language,
                existingTodos = existingTodosStr,
                existingEvents = existingEventsStr,
                generateNote = generateNote,
                generateTodos = generateTodos,
                defaultEventTime = defaultEventTime,
                isOfflineRetry = true
            ).collect { status ->
                finalStatus = status
            }

            if (finalStatus is AgentPipelineStatus.Success) {
                val res = (finalStatus as AgentPipelineStatus.Success).response
                
                var noteId: Long? = null
                if (generateNote && res.note != null) {
                    noteId = noteRepository.saveNote(
                        title = res.note.title,
                        summary = res.note.summary,
                        markdown = res.note.markdown,
                        tags = res.note.tags,
                        strokes = emptyList() // Fallback strokes if needed
                    )
                }

                if (generateTodos && res.todos.isNotEmpty()) {
                    val baseTimestamp = System.currentTimeMillis()
                    noteRepository.saveTodos(
                        res.todos.mapIndexed { idx, it ->
                            SavedTodo(
                                id = UUID.randomUUID().toString(),
                                text = it.text,
                                priority = it.priority,
                                dueDate = it.dueDate,
                                isCompleted = it.completed,
                                timestamp = baseTimestamp + idx,
                                noteId = noteId
                            )
                        }
                    )
                }
                
                // Cleanup files
                imagePaths.forEach { File(it).delete() }
                
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
