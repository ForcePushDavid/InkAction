package com.inkaction.app.ai

import com.google.gson.annotations.SerializedName

data class AgentResponse(
    @SerializedName("note") val note: NoteDto? = null,
    @SerializedName("todos") val todos: List<TodoDto> = emptyList(),
    @SerializedName("events") val events: List<EventDto> = emptyList(),
    @SerializedName("topics") val topics: List<TopicDto> = emptyList()
)

data class NoteDto(
    @SerializedName("title") val title: String = "Untitled S-Pen Note",
    @SerializedName("summary") val summary: String = "",
    @SerializedName("markdown") val markdown: String = "",
    @SerializedName("tags") val tags: List<String> = emptyList(),
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

data class TodoDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("text") val text: String = "",
    @SerializedName("priority") val priority: String = "medium",
    @SerializedName("dueDate") val dueDate: String = "",
    @SerializedName("completed") var completed: Boolean = false
)

data class EventDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("date") val date: String = "",
    @SerializedName("time") val time: String = "",
    @SerializedName("duration") val duration: String = "1h",
    @SerializedName("description") val description: String = "",
    @SerializedName("location") val location: String = ""
)

data class TopicDto(
    @SerializedName("name") val name: String = "",
    @SerializedName("summary") val summary: String = ""
)

sealed class AgentPipelineStatus {
    object Idle : AgentPipelineStatus()
    data class Processing(val step: String, val message: String) : AgentPipelineStatus()
    data class Success(val response: AgentResponse) : AgentPipelineStatus()
    data class Error(val message: String) : AgentPipelineStatus()
}
