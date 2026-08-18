package com.inkaction.app.ai

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GeminiAgentEngine(
    private var apiKey: String = "",
    private var modelName: String = "gemini-3.5-flash-lite"
) {
    private val gson = Gson()

    fun updateConfig(newApiKey: String, newModelName: String) {
        this.apiKey = newApiKey
        this.modelName = newModelName
    }

    /**
     * Executes multi-agent multimodal processing on handwritten bitmap
     */
    fun processInkBitmap(bitmap: Bitmap): Flow<AgentPipelineStatus> = flow {
        if (apiKey.isBlank()) {
            emit(AgentPipelineStatus.Processing("demo", "No API key configured - running Smart Demo..."))
            emit(runMockPipeline())
            return@flow
        }

        try {
            emit(AgentPipelineStatus.Processing("uploading", "Sending ink bitmap to Google Gemini..."))

            val generativeModel = GenerativeModel(
                modelName = modelName,
                apiKey = apiKey.trim(),
                generationConfig = generationConfig {
                    temperature = 0.2f
                    responseMimeType = "application/json"
                }
            )

            emit(AgentPipelineStatus.Processing("synthesizing", "Synthesizer Agent deciphering handwriting..."))

            val inputContent = content {
                text(AgentPrompts.MULTI_AGENT_SYSTEM_PROMPT)
                image(bitmap)
            }

            val response = generativeModel.generateContent(inputContent)
            val rawText = response.text

            if (rawText.isNullOrBlank()) {
                emit(AgentPipelineStatus.Error("Gemini returned empty response."))
                return@flow
            }

            emit(AgentPipelineStatus.Processing("extracting", "Extracting todos, dates & calendar events..."))

            val cleanedJson = cleanJsonString(rawText)
            val parsedResponse = gson.fromJson(cleanedJson, AgentResponse::class.java)

            emit(AgentPipelineStatus.Success(parsedResponse))

        } catch (e: Exception) {
            emit(AgentPipelineStatus.Error("AI Pipeline Error: ${e.localizedMessage}. Falling back to Smart Demo."))
            // Graceful fallback for smooth user experience
            emit(runMockPipeline())
        }
    }.flowOn(Dispatchers.IO)

    private fun cleanJsonString(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        }
        if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }

    private suspend fun runMockPipeline(): AgentPipelineStatus {
        delay(600)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrow = dateFormat.format(cal.time)
        
        cal.add(Calendar.DAY_OF_YEAR, 6)
        val nextWeek = dateFormat.format(cal.time)

        val mockResponse = AgentResponse(
            note = NoteDto(
                title = "Galaxy Tab S9 & S-Pen Architecture",
                summary = "Synthesized handwritten diagram into specifications, deliverables, and sprint review.",
                markdown = """
### S-Pen Multimodal Architecture
* **Low-latency Drawing**: Dynamic pressure scaling and Catmull-Rom smoothing.
* **Palm Rejection**: Rejects finger touches when S-Pen stylus is active.
* **Inactivity Auto-Push**: Automatically deploys 4 agents when idle for 6 seconds.

### Strategic Goals
1. Zero re-reading needed: Instant conversion to organized notes.
2. Direct sync to Android Calendar & Room Todo checklist.
3. Clean adaptive UI across Tab S9 and S26 Ultra.
                """.trimIndent(),
                tags = listOf("SPen", "GalaxyTabS9", "S26Ultra", "Architecture", "Gemini")
            ),
            todos = listOf(
                TodoDto(
                    id = "todo-1",
                    text = "Test S-Pen barrel button eraser toggle on Tab S9",
                    priority = "high",
                    dueDate = tomorrow,
                    completed = false
                ),
                TodoDto(
                    id = "todo-2",
                    text = "Configure Gemini API key in Android Settings dialog",
                    priority = "high",
                    dueDate = "Today",
                    completed = false
                ),
                TodoDto(
                    id = "todo-3",
                    text = "Review Android Calendar sync permission flow",
                    priority = "medium",
                    dueDate = nextWeek,
                    completed = false
                )
            ),
            events = listOf(
                EventDto(
                    id = "event-1",
                    title = "InkAction S-Pen Demo & Review",
                    date = tomorrow,
                    time = "14:00",
                    duration = "45m",
                    description = "Live demo of handwritten ink to calendar automation on Galaxy Tab S9.",
                    location = "Google Meet"
                ),
                EventDto(
                    id = "event-2",
                    title = "Sprint Retrospective",
                    date = nextWeek,
                    time = "16:30",
                    duration = "1h",
                    description = "Sprint retrospective and Galaxy S26 Ultra testing.",
                    location = "Main Workspace"
                )
            ),
            topics = listOf(
                TopicDto(
                    name = "S-Pen Engine",
                    summary = "Hardware-accelerated canvas with pressure sensitivity."
                ),
                TopicDto(
                    name = "Google Gemini Pipeline",
                    summary = "Multimodal AI for handwriting transcription, action items & scheduling."
                )
            )
        )
        return AgentPipelineStatus.Success(mockResponse)
    }
}
