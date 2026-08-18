package com.inkaction.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

data class SavedNote(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val summary: String,
    val markdown: String,
    val tags: List<String> = emptyList(),
    val strokes: List<com.inkaction.app.ui.canvas.InkStroke> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class SavedTodo(
    val id: String,
    val text: String,
    val priority: String,
    val dueDate: String,
    var isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class NoteStorageManager(private val context: Context) {

    private val gson = Gson()
    private val notesFile = File(context.filesDir, "saved_notes.json")
    private val todosFile = File(context.filesDir, "saved_todos.json")

    private val _notesFlow = MutableStateFlow<List<SavedNote>>(emptyList())
    val notesFlow: StateFlow<List<SavedNote>> = _notesFlow.asStateFlow()

    private val _todosFlow = MutableStateFlow<List<SavedTodo>>(emptyList())
    val todosFlow: StateFlow<List<SavedTodo>> = _todosFlow.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        try {
            if (notesFile.exists()) {
                val json = notesFile.readText()
                val type = object : TypeToken<List<SavedNote>>() {}.type
                val notes: List<SavedNote>? = gson.fromJson(json, type)
                if (notes != null) _notesFlow.value = notes
            }
            if (todosFile.exists()) {
                val json = todosFile.readText()
                val type = object : TypeToken<List<SavedTodo>>() {}.type
                val todos: List<SavedTodo>? = gson.fromJson(json, type)
                if (todos != null) _todosFlow.value = todos
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveNote(title: String, summary: String, markdown: String, tags: List<String>, strokes: List<com.inkaction.app.ui.canvas.InkStroke>): Long = withContext(Dispatchers.IO) {
        val note = SavedNote(
            id = System.currentTimeMillis(),
            title = title,
            summary = summary,
            markdown = markdown,
            tags = tags,
            strokes = strokes
        )
        val currentList = _notesFlow.value.toMutableList()
        currentList.add(0, note)
        _notesFlow.value = currentList
        try {
            notesFile.writeText(gson.toJson(currentList))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        note.id
    }

    suspend fun saveTodos(newTodos: List<SavedTodo>) = withContext(Dispatchers.IO) {
        val currentList = _todosFlow.value.toMutableList()
        // Prepend new todos
        currentList.addAll(0, newTodos)
        _todosFlow.value = currentList
        try {
            todosFile.writeText(gson.toJson(currentList))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateTodoCompletion(todoId: String, completed: Boolean) = withContext(Dispatchers.IO) {
        val currentList = _todosFlow.value.map {
            if (it.id == todoId) it.copy(isCompleted = completed) else it
        }
        _todosFlow.value = currentList
        try {
            todosFile.writeText(gson.toJson(currentList))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
