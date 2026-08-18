package com.inkaction.app.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inkaction.app.ai.AgentPipelineStatus
import com.inkaction.app.ai.AgentResponse
import com.inkaction.app.ai.EventDto
import com.inkaction.app.ai.GeminiAgentEngine
import com.inkaction.app.ai.NoteDto
import com.inkaction.app.ai.TodoDto
import com.inkaction.app.data.NoteStorageManager
import com.inkaction.app.data.SavedNote
import com.inkaction.app.data.SavedTodo
import com.inkaction.app.ui.canvas.ToolType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class AutoPushUiState(
    val isArmed: Boolean = false,
    val progress: Float = 0f,
    val remainingSeconds: Int = 0,
    val isProcessing: Boolean = false
)

class InkActionViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("inkaction_prefs", Context.MODE_PRIVATE)
    private val storageManager = NoteStorageManager(application)

    private val geminiEngine = GeminiAgentEngine()

    private val _currentTool = MutableStateFlow(ToolType.PEN)
    val currentTool: StateFlow<ToolType> = _currentTool.asStateFlow()

    private val _currentColor = MutableStateFlow(android.graphics.Color.parseColor("#F0F6FC"))
    val currentColor: StateFlow<Int> = _currentColor.asStateFlow()

    private val _autoPushState = MutableStateFlow(AutoPushUiState())
    val autoPushState: StateFlow<AutoPushUiState> = _autoPushState.asStateFlow()

    // Persist canvas strokes in ViewModel so they survive tab switches
    val currentStrokes = mutableListOf<com.inkaction.app.ui.canvas.InkStroke>()

    private val _pipelineStatus = MutableStateFlow<AgentPipelineStatus>(AgentPipelineStatus.Idle)
    val pipelineStatus: StateFlow<AgentPipelineStatus> = _pipelineStatus.asStateFlow()

    val allNotes: StateFlow<List<SavedNote>> = storageManager.notesFlow
    val foldersFlow: StateFlow<List<com.inkaction.app.data.NoteFolder>> = storageManager.foldersFlow

    private val _currentNote = MutableStateFlow<NoteDto?>(null)
    val currentNote: StateFlow<NoteDto?> = _currentNote.asStateFlow()

    val allTodos: StateFlow<List<SavedTodo>> = storageManager.todosFlow

    private val _events = MutableStateFlow<List<EventDto>>(emptyList())
    val events: StateFlow<List<EventDto>> = _events.asStateFlow()

    private var countdownJob: Job? = null
    var debounceDurationMs: Long = 600000L // Default 10 minutes
        private set
    var remindersEnabled: Boolean = false
        private set
    var noteLanguage: String = "Auto-detect"
        private set

    var apiKey: String = ""
        private set
    var modelName: String = "gemini-3.5-flash-lite"
        private set
    var themeMode by mutableStateOf("system") // "system", "dark", "light"
        private set

    var canvasTemplate by mutableStateOf(0)
        private set

    fun toggleTemplate() {
        canvasTemplate = (canvasTemplate + 1) % 4
    }

    init {
        loadSettings()
    }

    private fun loadSettings() {
        apiKey = prefs.getString("api_key", "") ?: ""
        modelName = prefs.getString("model_name", "gemini-3.5-flash-lite") ?: "gemini-3.5-flash-lite"
        remindersEnabled = prefs.getBoolean("reminders_enabled", false)
        noteLanguage = prefs.getString("note_language", "Auto-detect") ?: "Auto-detect"
        themeMode = prefs.getString("theme_mode", "system") ?: "system"
        // Natvrdo nastavíme 10 minut, aby se přepsaly případné starší uložené hodnoty
        debounceDurationMs = 600000L 
        prefs.edit().putLong("debounce_ms", 600000L).apply()
        
        geminiEngine.updateConfig(apiKey, modelName)
    }

    fun saveSettings(newKey: String, newModel: String, newDebounce: Long, reminders: Boolean, language: String, newThemeMode: String) {
        apiKey = newKey
        modelName = newModel
        debounceDurationMs = 600000L // Keep hardcoded 10 mins
        remindersEnabled = reminders
        noteLanguage = language
        themeMode = newThemeMode

        prefs.edit()
            .putString("api_key", newKey)
            .putString("model_name", newModel)
            .putLong("debounce_ms", 600000L)
            .putBoolean("reminders_enabled", reminders)
            .putString("note_language", language)
            .putString("theme_mode", newThemeMode)
            .apply()

        geminiEngine.updateConfig(newKey, newModel)
    }

    fun setTool(tool: ToolType) {
        _currentTool.value = tool
    }

    fun setColor(color: Int) {
        _currentColor.value = color
    }

    fun onStrokeStarted() {
        cancelCountdown()
    }

    fun onStrokeFinished(strokeCount: Int, getOcrBitmap: () -> Bitmap?) {
        if (strokeCount <= 0 || debounceDurationMs <= 0 || _autoPushState.value.isProcessing) {
            cancelCountdown()
            return
        }

        startCountdown(getOcrBitmap)
    }

    private fun startCountdown(getOcrBitmap: () -> Bitmap?) {
        cancelCountdown()
        countdownJob = viewModelScope.launch {
            val totalSteps = 60
            val stepDelay = (debounceDurationMs / totalSteps).coerceAtLeast(50)
            _autoPushState.value = AutoPushUiState(
                isArmed = true,
                progress = 0f,
                remainingSeconds = (debounceDurationMs / 1000).toInt()
            )

            for (i in 1..totalSteps) {
                delay(stepDelay)
                val progress = i.toFloat() / totalSteps.toFloat()
                val remainingMs = debounceDurationMs - (i * stepDelay)
                val remainingSec = (remainingMs / 1000).toInt().coerceAtLeast(0)

                _autoPushState.value = AutoPushUiState(
                    isArmed = true,
                    progress = progress,
                    remainingSeconds = remainingSec
                )
            }

            // Trigger AI Pipeline
            triggerActionize(getOcrBitmap())
        }
    }

    fun triggerActionize(bitmap: Bitmap?, strokes: List<com.inkaction.app.ui.canvas.InkStroke> = emptyList()) {
        cancelCountdown()
        if (bitmap == null) return

        _autoPushState.value = AutoPushUiState(isProcessing = true)

        viewModelScope.launch {
            geminiEngine.processInkBitmap(bitmap, noteLanguage).collect { status ->
                _pipelineStatus.value = status

                if (status is AgentPipelineStatus.Success) {
                    val res = status.response
                    _currentNote.value = res.note
                    _events.value = res.events
                    _autoPushState.value = AutoPushUiState(isProcessing = false)

                    // Persist to local storage
                    res.note?.let { note ->
                        storageManager.saveNote(
                            title = note.title,
                            summary = note.summary,
                            markdown = note.markdown,
                            tags = note.tags,
                            strokes = strokes
                        )
                    }
                    if (res.todos.isNotEmpty()) {
                        storageManager.saveTodos(
                            res.todos.map {
                                SavedTodo(
                                    id = it.id,
                                    text = it.text,
                                    priority = it.priority,
                                    dueDate = it.dueDate,
                                    isCompleted = it.completed
                                )
                            }
                        )
                    }
                } else if (status is AgentPipelineStatus.Error) {
                    _autoPushState.value = AutoPushUiState(isProcessing = false)
                }
            }
        }
    }

    fun toggleTodo(todoId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            storageManager.updateTodoCompletion(todoId, !currentStatus)
        }
    }

    fun deleteTodo(todoId: String) {
        viewModelScope.launch {
            storageManager.deleteTodo(todoId)
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            storageManager.deleteNote(noteId)
        }
    }

    fun toggleNotePin(noteId: Long) {
        viewModelScope.launch {
            storageManager.toggleNotePin(noteId)
        }
    }

    fun createFolder(name: String, colorHex: String = "#38BDF8") {
        viewModelScope.launch {
            storageManager.createFolder(name, colorHex)
        }
    }

    fun moveNoteToFolder(noteId: Long, folderId: Long?) {
        viewModelScope.launch {
            storageManager.updateNoteFolder(noteId, folderId)
        }
    }

    private val _enhancingNotes = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val enhancingNotes: StateFlow<Map<Long, Boolean>> = _enhancingNotes.asStateFlow()

    fun enhanceNote(noteId: Long) {
        viewModelScope.launch {
            _enhancingNotes.value = _enhancingNotes.value.toMutableMap().apply { put(noteId, true) }
            val note = allNotes.value.find { it.id == noteId }
            if (note != null) {
                val enhancedText = geminiEngine.enhanceNoteContent(note.summary, note.markdown, noteLanguage)
                if (enhancedText != null) {
                    storageManager.updateNoteEnhancement(noteId, enhancedText)
                }
            }
            _enhancingNotes.value = _enhancingNotes.value.toMutableMap().apply { remove(noteId) }
        }
    }

    fun loadNoteToCanvas(note: SavedNote) {
        currentStrokes.clear()
        currentStrokes.addAll(note.strokes)
        _currentNote.value = NoteDto(
            title = note.title,
            summary = note.summary,
            markdown = note.markdown,
            tags = note.tags,
            timestamp = note.timestamp
        )
    }

    fun createNewNote() {
        currentStrokes.clear()
        _currentNote.value = null
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        if (!_autoPushState.value.isProcessing) {
            _autoPushState.value = AutoPushUiState()
        }
    }
}
