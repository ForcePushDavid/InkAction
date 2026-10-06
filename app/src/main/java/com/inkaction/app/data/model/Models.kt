package com.inkaction.app.data.model

import com.inkaction.app.ui.canvas.InkStroke

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
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val folderId: Long? = null,
    val aiEnhancement: String? = null,
    val isArchived: Boolean = false
)

data class SavedTodo(
    val id: String,
    val text: String,
    val priority: String,
    val dueDate: String,
    var isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val noteId: Long? = null,
    val isArchived: Boolean = false
)

data class MigrationNote(
    val id: Long,
    val strokes: List<InkStroke>? = null
)
