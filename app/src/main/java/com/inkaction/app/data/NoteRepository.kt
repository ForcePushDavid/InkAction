package com.inkaction.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.inkaction.app.data.model.MigrationNote
import com.inkaction.app.data.model.NoteFolder
import com.inkaction.app.data.model.SavedNote
import com.inkaction.app.data.model.SavedTodo
import com.inkaction.app.ui.canvas.InkStroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class NoteRepository(private val context: Context) {
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

    private val notesMutex = Mutex()
    private val todosMutex = Mutex()
    private val foldersMutex = Mutex()

    init {
        loadData()
    }

    private fun atomicWrite(file: File, content: String) {
        val tempFile = File(file.parent, "${file.name}.tmp")
        tempFile.writeText(content)
        tempFile.renameTo(file)
    }

    private fun loadData() {
        try {
            if (notesFile.exists()) {
                try {
                    var needsMigration = false
                    java.io.FileReader(notesFile).use { reader ->
                        val migrationType = object : TypeToken<List<MigrationNote>>() {}.type
                        val migrationNotes: List<MigrationNote>? = gson.fromJson(reader, migrationType)
                        migrationNotes?.forEach { migrationNote ->
                            if (!migrationNote.strokes.isNullOrEmpty()) {
                                val strokesFile = File(context.filesDir, "strokes_${migrationNote.id}.json")
                                if (!strokesFile.exists()) {
                                    atomicWrite(strokesFile, gson.toJson(migrationNote.strokes))
                                }
                                needsMigration = true
                            }
                        }
                    }

                    if (needsMigration) {
                        java.io.FileReader(notesFile).use { reader ->
                            val type = object : TypeToken<List<SavedNote>>() {}.type
                            val notesWithoutStrokes: List<SavedNote>? = gson.fromJson(reader, type)
                            if (notesWithoutStrokes != null) {
                                atomicWrite(notesFile, gson.toJson(notesWithoutStrokes))
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                java.io.FileReader(notesFile).use { reader ->
                    val type = object : TypeToken<List<SavedNote>>() {}.type
                    val notes: List<SavedNote>? = gson.fromJson(reader, type)
                    if (notes != null) _notesFlow.value = notes
                }
            }
            if (todosFile.exists()) {
                val json = todosFile.readText()
                val type = object : TypeToken<List<SavedTodo>>() {}.type
                val rawTodos: List<SavedTodo>? = gson.fromJson(json, type)
                if (rawTodos != null) {
                    val seenIds = mutableSetOf<String>()
                    val sanitizedTodos = rawTodos.mapIndexed { index, todo ->
                        val uniqueId = if (todo.id.isBlank() || seenIds.contains(todo.id)) {
                            "${todo.id.ifBlank { "todo" }}_${System.currentTimeMillis()}_$index"
                        } else {
                            todo.id
                        }
                        seenIds.add(uniqueId)
                        todo.copy(id = uniqueId)
                    }
                    _todosFlow.value = sanitizedTodos
                }
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

    suspend fun updateNotes(block: (MutableList<SavedNote>) -> Unit) = withContext(Dispatchers.IO) {
        notesMutex.withLock {
            val currentList = _notesFlow.value.toMutableList()
            block(currentList)
            _notesFlow.value = currentList
            try {
                atomicWrite(notesFile, gson.toJson(currentList))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun updateTodos(block: (MutableList<SavedTodo>) -> Unit) = withContext(Dispatchers.IO) {
        todosMutex.withLock {
            val currentList = _todosFlow.value.toMutableList()
            block(currentList)
            _todosFlow.value = currentList
            try {
                atomicWrite(todosFile, gson.toJson(currentList))
                updateWidget()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun updateFolders(block: (MutableList<NoteFolder>) -> Unit) = withContext(Dispatchers.IO) {
        foldersMutex.withLock {
            val currentList = _foldersFlow.value.toMutableList()
            block(currentList)
            _foldersFlow.value = currentList
            try {
                atomicWrite(foldersFile, gson.toJson(currentList))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun saveNote(title: String, summary: String, markdown: String, tags: List<String>, strokes: List<InkStroke>): Long {
        val note = SavedNote(
            id = System.currentTimeMillis(),
            title = title,
            summary = summary,
            markdown = markdown,
            tags = tags
        )
        updateNotes { it.add(0, note) }
        saveStrokes(note.id, strokes)
        return note.id
    }

    suspend fun updateNote(noteId: Long, title: String, summary: String, markdown: String, tags: List<String>, strokes: List<InkStroke>) {
        updateNotes { list ->
            val index = list.indexOfFirst { it.id == noteId }
            if (index != -1) {
                list[index] = list[index].copy(
                    title = title,
                    summary = summary,
                    markdown = markdown,
                    tags = tags,
                    timestamp = System.currentTimeMillis()
                )
            }
        }
        saveStrokes(noteId, strokes)
    }

    suspend fun saveTodos(newTodos: List<SavedTodo>) {
        updateTodos { it.addAll(0, newTodos) }
    }

    suspend fun updateTodoCompletion(todoId: String, completed: Boolean) {
        updateTodos { list ->
            val index = list.indexOfFirst { it.id == todoId }
            if (index != -1) {
                list[index] = list[index].copy(isCompleted = completed)
            }
        }
    }

    suspend fun deleteTodo(todoId: String) {
        updateTodos { it.removeAll { todo -> todo.id == todoId } }
    }

    suspend fun deleteNotePermanently(noteId: Long) {
        updateNotes { it.removeAll { note -> note.id == noteId } }
        withContext(Dispatchers.IO) {
            val strokesFile = File(context.filesDir, "strokes_$noteId.json")
            if (strokesFile.exists()) {
                strokesFile.delete()
            }
        }
    }

    suspend fun archiveNote(noteId: Long, archived: Boolean = true) {
        updateNotes { list ->
            val index = list.indexOfFirst { it.id == noteId }
            if (index != -1) {
                list[index] = list[index].copy(isArchived = archived)
            }
        }
    }

    suspend fun archiveTodo(todoId: String, archived: Boolean = true) {
        updateTodos { list ->
            val index = list.indexOfFirst { it.id == todoId }
            if (index != -1) {
                list[index] = list[index].copy(isArchived = archived)
            }
        }
    }

    suspend fun toggleNotePin(noteId: Long) {
        updateNotes { list ->
            val index = list.indexOfFirst { it.id == noteId }
            if (index != -1) {
                list[index] = list[index].copy(isPinned = !list[index].isPinned)
            }
        }
    }

    suspend fun createFolder(name: String, colorHex: String) {
        val folder = NoteFolder(name = name, colorHex = colorHex)
        updateFolders { it.add(folder) }
    }

    suspend fun updateNoteFolder(noteId: Long, folderId: Long?) {
        updateNotes { list ->
            val index = list.indexOfFirst { it.id == noteId }
            if (index != -1) {
                list[index] = list[index].copy(folderId = folderId)
            }
        }
    }

    suspend fun updateNoteEnhancement(noteId: Long, enhancement: String) {
        updateNotes { list ->
            val index = list.indexOfFirst { it.id == noteId }
            if (index != -1) {
                list[index] = list[index].copy(aiEnhancement = enhancement)
            }
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

    suspend fun saveStrokes(noteId: Long, strokes: List<InkStroke>) = withContext(Dispatchers.IO) {
        try {
            val strokesFile = File(context.filesDir, "strokes_$noteId.json")
            atomicWrite(strokesFile, gson.toJson(strokes))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadStrokes(noteId: Long): List<InkStroke> = withContext(Dispatchers.IO) {
        try {
            val strokesFile = File(context.filesDir, "strokes_$noteId.json")
            if (strokesFile.exists()) {
                val type = object : TypeToken<List<InkStroke>>() {}.type
                return@withContext gson.fromJson(strokesFile.readText(), type) ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }
}
