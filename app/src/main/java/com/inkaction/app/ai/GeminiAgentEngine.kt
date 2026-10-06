package com.inkaction.app.ai

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GeminiAgentEngine(
    private var apiKey: String = "",
    private var modelName: String = "gemini-3.8-flash"
) {
    private val gson = Gson()

    fun updateConfig(newApiKey: String, newModelName: String) {
        this.apiKey = newApiKey
        this.modelName = newModelName
    }

    /**
     * Executes multi-agent multimodal processing on handwritten bitmap
     */
    fun processInkBitmap(
        bitmaps: List<Bitmap>,
        language: String,
        existingTodos: String = "",
        existingEvents: String = "",
        generateNote: Boolean = true,
        generateTodos: Boolean = true,
        defaultEventTime: String = "08:00",
        isOfflineRetry: Boolean = false
    ): Flow<AgentPipelineStatus> = flow {
        if (apiKey.isBlank()) {
            emit(AgentPipelineStatus.Error(PipelineError.MissingApiKey.message))
            return@flow
        }

        try {
            emit(AgentPipelineStatus.Processing("uploading", "Sending ink pages to Google Gemini..."))

            val generativeModel = GenerativeModel(
                modelName = modelName,
                apiKey = apiKey.trim(),
                generationConfig = generationConfig {
                    temperature = 0.2f
                    responseMimeType = "application/json"
                }
            )

            emit(AgentPipelineStatus.Processing("synthesizing", "Synthesizer Agent deciphering handwriting..."))

            val currentDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val languageInstruction = if (language != "Auto-detect") "Output language strictly MUST BE: $language" else "Auto-detect the language from the handwriting."
            
            var deduplicationPrompt = ""
            if (existingTodos.isNotBlank() || existingEvents.isNotBlank()) {
                deduplicationPrompt = "\n\nDEDUPLICATION DIRECTIVE: The user already has the following items extracted from previous sessions. DO NOT output these again. Only output NEW events and NEW todos.\nExisting Todos: $existingTodos\nExisting Events: $existingEvents"
            }
            
            val noteDirective = if (!generateNote) "\n\nCRITICAL DIRECTIVE: DO NOT GENERATE A NOTE. The user explicitly requested to skip note generation. Set the 'note' object in JSON to null. ONLY extract todos, events, and topics." else ""
            val todosDirective = if (!generateTodos) "\n\nCRITICAL DIRECTIVE: DO NOT EXTRACT TODOS. The user explicitly requested to skip todo generation. Set the 'todos' array in JSON to an empty array []. ONLY extract note, events, and topics." else ""
            
            val calendarDirective = """
                
                CALENDAR AND TODO DIRECTIVE:
                1. For ANY date you extract (in events OR todos), you MUST strictly convert it to 'YYYY-MM-DD' format based on the current date context. Do not output words like 'tomorrow'.
                2. If an event or a timed-todo does not have a specific time of day mentioned, use the user's default time: $defaultEventTime.
                3. If you extract a Todo that contains ANY time expression or deadline (e.g., 'tomorrow afternoon', 'today', 'Friday'), you MUST ALSO create an entry for it in the 'events' array so the user receives a calendar reminder.
            """.trimIndent()
            
            val fullPrompt = "${AgentPrompts.MULTI_AGENT_SYSTEM_PROMPT}\n\nCurrent Date and Time: $currentDateTime\nUse this current date and time for interpreting relative dates like 'tomorrow' or 'next friday', and include it in the synthesized note summary or title if appropriate.\n\nLanguage Directive: $languageInstruction$deduplicationPrompt$noteDirective$todosDirective$calendarDirective"

            val inputContent = content {
                text(fullPrompt)
                bitmaps.forEach { image(it) }
            }

            val response = generativeModel.generateContent(inputContent)
            val rawText = response.text

            if (rawText.isNullOrBlank()) {
                emit(AgentPipelineStatus.Error(PipelineError.EmptyResponse.message))
                return@flow
            }

            emit(AgentPipelineStatus.Processing("extracting", "Extracting todos, dates & calendar events..."))

            val cleanedJson = cleanJsonString(rawText)
            val parsedResponse = gson.fromJson(cleanedJson, AgentResponse::class.java)

            emit(AgentPipelineStatus.Success(parsedResponse))

        } catch (e: Exception) {
            emit(AgentPipelineStatus.Error(PipelineError.Network(e.localizedMessage ?: "Unknown error").message))
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

    suspend fun enhanceNoteContent(originalNote: String, markdown: String, language: String): String? {
        if (apiKey.isBlank()) {
            return null
        }
        try {
            val generativeModel = GenerativeModel(
                modelName = modelName,
                apiKey = apiKey.trim(),
                generationConfig = generationConfig {
                    temperature = 0.4f
                }
            )
            val languageInstruction = if (language != "Auto-detect") "Please provide the output in $language." else ""
            val prompt = """
                Analyze the following note. Provide actionable next steps, deeper insights, missing context, or relevant follow-up questions.
                Format the response nicely in Markdown.
                $languageInstruction
                
                Original Summary:
                $originalNote
                
                Note Markdown:
                $markdown
            """.trimIndent()
            
            val response = generativeModel.generateContent(prompt)
            return response.text
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
