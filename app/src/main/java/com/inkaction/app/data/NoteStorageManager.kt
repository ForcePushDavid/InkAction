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

data class NoteFolder(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val colorHex: String = "#38BDF8"
)

data class SavedNote(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val summary: String,
    val markdown: String,
    val tags: List<String> = emptyList(),
    val strokes: List<com.inkaction.app.ui.canvas.InkStroke> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val folderId: Long? = null,
    val aiEnhancement: String? = null
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
    private val foldersFile = File(context.filesDir, "saved_folders.json")

    private val _notesFlow = MutableStateFlow<List<SavedNote>>(emptyList())
    val notesFlow: StateFlow<List<SavedNote>> = _notesFlow.asStateFlow()

    private val _todosFlow = MutableStateFlow<List<SavedTodo>>(emptyList())
    val todosFlow: StateFlow<List<SavedTodo>> = _todosFlow.asStateFlow()

    private val _foldersFlow = MutableStateFlow<List<NoteFolder>>(emptyList())
    val foldersFlow: StateFlow<List<NoteFolder>> = _foldersFlow.asStateFlow()

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
            if (foldersFile.exists()) {
                val json = foldersFile.readText()
                val type = object : TypeToken<List<NoteFolder>>() {}.type
                val folders: List<NoteFolder>? = gson.fromJson(json, type)
                if (folders != null) _foldersFlow.value = folders
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
            updateWidget()
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
            updateWidget()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteTodo(todoId: String) = withContext(Dispatchers.IO) {
        val currentList = _todosFlow.value.filter { it.id != todoId }
        _todosFlow.value = currentList
        try {
            todosFile.writeText(gson.toJson(currentList))
            updateWidget()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteNote(noteId: Long) = withContext(Dispatchers.IO) {
        val currentList = _notesFlow.value.filter { it.id != noteId }
        _notesFlow.value = currentList
        try {
            notesFile.writeText(gson.toJson(currentList))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun toggleNotePin(noteId: Long) = withContext(Dispatchers.IO) {
        val currentList = _notesFlow.value.map {
            if (it.id == noteId) it.copy(isPinned = !it.isPinned) else it
        }
        _notesFlow.value = currentList
        try {
            notesFile.writeText(gson.toJson(currentList))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun createFolder(name: String, colorHex: String) = withContext(Dispatchers.IO) {
        val folder = NoteFolder(name = name, colorHex = colorHex)
        val currentList = _foldersFlow.value.toMutableList()
        currentList.add(folder)
        _foldersFlow.value = currentList
        try {
            foldersFile.writeText(gson.toJson(currentList))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateNoteFolder(noteId: Long, folderId: Long?) = withContext(Dispatchers.IO) {
        val currentList = _notesFlow.value.map {
            if (it.id == noteId) it.copy(folderId = folderId) else it
        }
        _notesFlow.value = currentList
        try {
            notesFile.writeText(gson.toJson(currentList))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateNoteEnhancement(noteId: Long, enhancement: String) = withContext(Dispatchers.IO) {
        val currentList = _notesFlow.value.map {
            if (it.id == noteId) it.copy(aiEnhancement = enhancement) else it
        }
        _notesFlow.value = currentList
        try {
            notesFile.writeText(gson.toJson(currentList))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateWidget() {
        try {
            val intent = android.content.Intent(context, com.inkaction.app.widget.TodoWidgetProvider::class.java).apply {
                action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(android.content.ComponentName(context, com.inkaction.app.widget.TodoWidgetProvider::class.java))
            intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
            appWidgetManager.notifyAppWidgetViewDataChanged(ids, com.inkaction.app.R.id.widget_list)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
