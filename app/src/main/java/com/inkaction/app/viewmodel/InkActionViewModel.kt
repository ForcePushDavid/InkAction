package com.inkaction.app.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inkaction.app.InkActionApp
import com.inkaction.app.ai.AgentPipelineStatus
import com.inkaction.app.ai.EventDto
import com.inkaction.app.ai.GeminiAgentEngine
import com.inkaction.app.ai.NoteDto
import com.inkaction.app.data.model.SavedNote
import com.inkaction.app.data.model.SavedTodo
import com.inkaction.app.data.model.NoteFolder
import com.inkaction.app.ui.canvas.InkStroke
import com.inkaction.app.ui.canvas.ToolType
import com.inkaction.app.worker.OfflineQueue
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class AutoPushUiState(
    val isArmed: Boolean = false,
    val progress: Float = 0f,
    val remainingSeconds: Int = 0,
    val isProcessing: Boolean = false
)

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
}

class InkActionViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as InkActionApp
    private val noteRepository = app.noteRepository
    private val settingsRepository = app.settingsRepository

    val settings = settingsRepository.settings

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private val geminiEngine = GeminiAgentEngine(settings.value.apiKey, settings.value.modelName)

    private val _currentTool = MutableStateFlow(ToolType.PEN)
    val currentTool: StateFlow<ToolType> = _currentTool.asStateFlow()

    private val _currentColor = MutableStateFlow(android.graphics.Color.parseColor("#F0F6FC"))
    val currentColor: StateFlow<Int> = _currentColor.asStateFlow()

    private val _autoPushState = MutableStateFlow(AutoPushUiState())
    val autoPushState: StateFlow<AutoPushUiState> = _autoPushState.asStateFlow()

    val currentStrokes = mutableListOf<InkStroke>()

    var currentNoteId: Long? = null
        private set

    private val _pipelineStatus = MutableStateFlow<AgentPipelineStatus>(AgentPipelineStatus.Idle)
    val pipelineStatus: StateFlow<AgentPipelineStatus> = _pipelineStatus.asStateFlow()

    val allNotes: StateFlow<List<SavedNote>> = noteRepository.notesFlow
    val foldersFlow: StateFlow<List<NoteFolder>> = noteRepository.foldersFlow

    private val _currentNote = MutableStateFlow<NoteDto?>(null)
    val currentNote: StateFlow<NoteDto?> = _currentNote.asStateFlow()

    val allTodos: StateFlow<List<SavedTodo>> = noteRepository.todosFlow

    private val _events = MutableStateFlow<List<EventDto>>(emptyList())
    val events: StateFlow<List<EventDto>> = _events.asStateFlow()

    private var countdownJob: Job? = null
    private var autoSaveJob: Job? = null

    var canvasTemplate by mutableStateOf(0)
        private set

    init {
        viewModelScope.launch {
            settings.collect { currentSettings ->
                geminiEngine.updateConfig(currentSettings.apiKey, currentSettings.modelName)
            }
        }
    }

    fun toggleTemplate() {
        canvasTemplate = (canvasTemplate + 1) % 4
    }

    fun saveSettings(
        apiKey: String,
        modelName: String,
        remindersEnabled: Boolean,
        noteLanguage: String,
        themeMode: String,
        defaultEventTime: String
    ) {
        settingsRepository.saveSettings(apiKey, modelName, remindersEnabled, noteLanguage, themeMode, defaultEventTime)
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

    fun onStrokeFinished(strokeCount: Int, getOcrBitmaps: () -> List<Bitmap>) {
        if (strokeCount <= 0 || settings.value.debounceDurationMs <= 0 || _autoPushState.value.isProcessing) {
            cancelCountdown()
            return
        }

        startCountdown(getOcrBitmaps)
        
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1500)
            saveCurrentNoteManually()
        }
    }

    private fun startCountdown(getOcrBitmaps: () -> List<Bitmap>) {
        cancelCountdown()
        countdownJob = viewModelScope.launch {
            val totalSteps = 60
            val debounceMs = settings.value.debounceDurationMs
            val stepDelay = (debounceMs / totalSteps).coerceAtLeast(50)
            
            _autoPushState.value = AutoPushUiState(
                isArmed = true,
                progress = 0f,
                remainingSeconds = (debounceMs / 1000).toInt()
            )

            for (i in 1..totalSteps) {
                delay(stepDelay)
                val progress = i.toFloat() / totalSteps.toFloat()
                val remainingMs = debounceMs - (i * stepDelay)
                val remainingSec = (remainingMs / 1000).toInt().coerceAtLeast(0)

                _autoPushState.value = AutoPushUiState(
                    isArmed = true,
                    progress = progress,
                    remainingSeconds = remainingSec
                )
            }

            triggerActionize(getOcrBitmaps())
        }
    }

    fun triggerActionize(bitmaps: List<Bitmap>, strokes: List<InkStroke> = emptyList(), generateNote: Boolean = true, generateTodos: Boolean = true) {
        cancelCountdown()
        if (bitmaps.isEmpty()) return

        _autoPushState.value = AutoPushUiState(isProcessing = true)

        val currentSettings = settings.value
        val existingTodosStr = allTodos.value.filter { !it.isCompleted }.joinToString(", ") { it.text }
        val existingEventsStr = events.value.joinToString(", ") { "${it.title} on ${it.date}" }

        viewModelScope.launch {
            geminiEngine.processInkBitmap(bitmaps, currentSettings.noteLanguage, existingTodosStr, existingEventsStr, generateNote, generateTodos, currentSettings.defaultEventTime).collect { status ->
                _pipelineStatus.value = status

                when (status) {
                    is AgentPipelineStatus.Success -> {
                        val res = status.response
                        
                        if (generateNote) {
                            _currentNote.value = res.note
                        }
                        
                        _events.value = res.events
                        _autoPushState.value = AutoPushUiState(isProcessing = false)

                        if (res.events.isNotEmpty()) {
                            res.events.forEach { event ->
                                com.inkaction.app.util.CalendarSyncUtil.addEventToCalendar(getApplication(), event)
                            }
                        }
                        
                        if (generateNote) {
                            res.note?.let { note ->
                                if (currentNoteId != null) {
                                    noteRepository.updateNote(
                                        noteId = currentNoteId!!,
                                        title = note.title,
                                        summary = note.summary,
                                        markdown = note.markdown,
                                        tags = note.tags,
                                        strokes = strokes
                                    )
                                } else {
                                    currentNoteId = noteRepository.saveNote(
                                        title = note.title,
                                        summary = note.summary,
                                        markdown = note.markdown,
                                        tags = note.tags,
                                        strokes = strokes
                                    )
                                }
                            }
                        }
                        if (generateTodos && res.todos.isNotEmpty()) {
                            val baseTimestamp = System.currentTimeMillis()
                            noteRepository.saveTodos(
                                res.todos.mapIndexed { idx, it ->
                                    SavedTodo(
                                        id = if (it.id.isNotBlank() && it.id.startsWith("todo-")) "${it.id}_${baseTimestamp}_$idx" else UUID.randomUUID().toString(),
                                        text = it.text,
                                        priority = it.priority,
                                        dueDate = it.dueDate,
                                        isCompleted = it.completed,
                                        timestamp = baseTimestamp + idx,
                                        noteId = currentNoteId
                                    )
                                }
                            )
                        }
                    }
                    is AgentPipelineStatus.Error -> {
                        _autoPushState.value = AutoPushUiState(isProcessing = false)
                        if (currentSettings.apiKey.isNotBlank()) {
                            _uiEvent.send(UiEvent.ShowSnackbar(status.message))
                            OfflineQueue.enqueue(
                                context = getApplication(),
                                bitmaps = bitmaps,
                                apiKey = currentSettings.apiKey,
                                modelName = currentSettings.modelName,
                                noteLanguage = currentSettings.noteLanguage,
                                defaultEventTime = currentSettings.defaultEventTime,
                                existingTodosStr = existingTodosStr,
                                existingEventsStr = existingEventsStr,
                                generateNote = generateNote,
                                generateTodos = generateTodos
                            )
                        } else {
                            _uiEvent.send(UiEvent.ShowSnackbar("Missing API Key. Please update in settings."))
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun toggleTodo(todoId: String, newStatus: Boolean) {
        viewModelScope.launch {
            noteRepository.updateTodoCompletion(todoId, newStatus)
        }
    }

    fun deleteTodo(todoId: String) {
        viewModelScope.launch {
            noteRepository.deleteTodo(todoId)
        }
    }

    fun archiveTodo(todoId: String) {
        viewModelScope.launch {
            noteRepository.archiveTodo(todoId, true)
            _uiEvent.send(UiEvent.ShowSnackbar("Todo archived"))
        }
    }

    fun unarchiveTodo(todoId: String) {
        viewModelScope.launch {
            noteRepository.archiveTodo(todoId, false)
            _uiEvent.send(UiEvent.ShowSnackbar("Todo unarchived"))
        }
    }

    fun deleteNotePermanently(noteId: Long) {
        viewModelScope.launch {
            noteRepository.deleteNotePermanently(noteId)
        }
    }

    fun archiveNote(noteId: Long) {
        viewModelScope.launch {
            noteRepository.archiveNote(noteId, true)
            _uiEvent.send(UiEvent.ShowSnackbar("Note archived"))
        }
    }

    fun unarchiveNote(noteId: Long) {
        viewModelScope.launch {
            noteRepository.archiveNote(noteId, false)
            _uiEvent.send(UiEvent.ShowSnackbar("Note unarchived"))
        }
    }

    fun toggleNotePin(noteId: Long) {
        viewModelScope.launch {
            noteRepository.toggleNotePin(noteId)
        }
    }

    fun createFolder(name: String, colorHex: String = "#38BDF8") {
        viewModelScope.launch {
            noteRepository.createFolder(name, colorHex)
        }
    }

    fun moveNoteToFolder(noteId: Long, folderId: Long?) {
        viewModelScope.launch {
            noteRepository.updateNoteFolder(noteId, folderId)
        }
    }

    private val _enhancingNotes = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val enhancingNotes: StateFlow<Map<Long, Boolean>> = _enhancingNotes.asStateFlow()

    fun enhanceNote(noteId: Long) {
        viewModelScope.launch {
            _enhancingNotes.value = _enhancingNotes.value.toMutableMap().apply { put(noteId, true) }
            val note = allNotes.value.find { it.id == noteId }
            if (note != null) {
                val enhancedText = geminiEngine.enhanceNoteContent(note.summary, note.markdown, settings.value.noteLanguage)
                if (enhancedText != null) {
                    noteRepository.updateNoteEnhancement(noteId, enhancedText)
                } else {
                    _uiEvent.send(UiEvent.ShowSnackbar("Failed to enhance note"))
                }
            }
            _enhancingNotes.value = _enhancingNotes.value.toMutableMap().apply { remove(noteId) }
        }
    }

    fun loadNoteToCanvas(note: SavedNote) {
        currentStrokes.clear()
        currentNoteId = note.id
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
        currentNoteId = null
        _currentNote.value = null
    }

    fun loadStrokesForNote(noteId: Long, onLoaded: (List<InkStroke>) -> Unit) {
        viewModelScope.launch {
            val strokes = noteRepository.loadStrokes(noteId)
            currentStrokes.clear()
            currentStrokes.addAll(strokes)
            onLoaded(strokes)
        }
    }

    fun saveCurrentNoteManually() {
        viewModelScope.launch {
            if (currentNoteId != null) {
                noteRepository.updateNote(
                    noteId = currentNoteId!!,
                    title = _currentNote.value?.title ?: "Draft",
                    summary = _currentNote.value?.summary ?: "",
                    markdown = _currentNote.value?.markdown ?: "",
                    tags = _currentNote.value?.tags ?: emptyList(),
                    strokes = currentStrokes.toList()
                )
            } else {
                currentNoteId = noteRepository.saveNote(
                    title = "Draft Note",
                    summary = "Manually saved without AI.",
                    markdown = "",
                    tags = emptyList(),
                    strokes = currentStrokes.toList()
                )
            }
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        if (!_autoPushState.value.isProcessing) {
            _autoPushState.value = AutoPushUiState()
        }
    }
}
