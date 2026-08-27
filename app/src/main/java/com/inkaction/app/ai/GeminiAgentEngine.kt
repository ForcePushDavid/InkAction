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
    fun processInkBitmap(bitmaps: List<Bitmap>, language: String, existingTodos: String = "", existingEvents: String = ""): Flow<AgentPipelineStatus> = flow {
        if (apiKey.isBlank()) {
            emit(AgentPipelineStatus.Error("Gemini API kľúč nie je nastavený. Prosím, nastavte ho v nastaveniach."))
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

            val currentDateTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            val languageInstruction = if (language != "Auto-detect") "Output language strictly MUST BE: $language" else "Auto-detect the language from the handwriting."
            
            var deduplicationPrompt = ""
            if (existingTodos.isNotBlank() || existingEvents.isNotBlank()) {
                deduplicationPrompt = "\n\nDEDUPLICATION DIRECTIVE: The user already has the following items extracted from previous sessions. DO NOT output these again. Only output NEW events and NEW todos.\nExisting Todos: $existingTodos\nExisting Events: $existingEvents"
            }
            
            val fullPrompt = "${AgentPrompts.MULTI_AGENT_SYSTEM_PROMPT}\n\nCurrent Date and Time: $currentDateTime\nUse this current date and time for interpreting relative dates like 'tomorrow' or 'next friday', and include it in the synthesized note summary or title if appropriate.\nAlso, ANY mention of a date, deadline, meeting, or time MUST be added to the 'events' array so the user can be suggested to add it to their calendar.\n\nLanguage Directive: $languageInstruction$deduplicationPrompt"

            val inputContent = content {
                text(fullPrompt)
                bitmaps.forEach { image(it) }
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

    private var mockIndex = 0

    private suspend fun runMockPipeline(): AgentPipelineStatus {
        delay(600)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrow = dateFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 6)
        val nextWeek = dateFormat.format(cal.time)

        val currentIndex = mockIndex++ % 4

        val mockResponse = when (currentIndex) {
            0 -> AgentResponse(
                note = NoteDto(
                    title = "Brainstorm: Produktova strategie Q3",
                    summary = "Shrnuti klicovych bodu z brainstormingu o produktove strategii.",
                    markdown = "### Klicove body\n* **Cilova skupina**: Studenti a kreativci\n* **Konkurencni vyhoda**: AI rozpoznavani rukopisu v realnem case\n* **Dalsi kroky**: Pripravit MVP pro beta testery\n\n### Napady\n1. Integrace s Google Calendar\n2. Export do PDF jednim klikem\n3. Sdileni poznamek pres QR kod",
                    tags = listOf("Strategie", "Produkt", "Q3", "Brainstorm")
                ),
                todos = listOf(
                    TodoDto("todo-1", "Pripravit prezentaci pro investory", "high", tomorrow, false),
                    TodoDto("todo-2", "Kontaktovat beta testery", "medium", nextWeek, false)
                ),
                events = listOf(
                    EventDto("event-1", "Produktovy review", tomorrow, "10:00", "1h", "Revize produktove strategie s tymem.", "Kancelar")
                ),
                topics = listOf(TopicDto("Produktova strategie", "Planovani smeru produktu pro dalsi kvartal."))
            )
            1 -> AgentResponse(
                note = NoteDto(
                    title = "Poznamky z prednasky: Strojove uceni",
                    summary = "Klicove koncepty z prednasky o zakladech strojoveho uceni.",
                    markdown = "### Zaklady ML\n* **Supervised Learning**: Uceni s oznacenymi daty\n* **Unsupervised Learning**: Hledani vzoru v neoznacenych datech\n* **Neural Networks**: Vrstvy neuronu inspirovane mozkem\n\n### Dulezite vzorce\n- Loss function: Meri chybu modelu\n- Gradient descent: Optimalizace vah\n- Overfitting vs Underfitting",
                    tags = listOf("ML", "AI", "Prednaska", "Uceni")
                ),
                todos = listOf(
                    TodoDto("todo-1", "Procvicit gradient descent na prikladu", "high", tomorrow, false),
                    TodoDto("todo-2", "Precist kapitolu 5 z ucebnice ML", "medium", nextWeek, false),
                    TodoDto("todo-3", "Napsat shrnuti pro studijni skupinu", "low", nextWeek, false)
                ),
                events = listOf(
                    EventDto("event-1", "Dalsi prednaska ML", nextWeek, "09:00", "2h", "Pokrocile neuronove site a CNN.", "Aula B3")
                ),
                topics = listOf(TopicDto("Strojove uceni", "Zaklady supervised a unsupervised learningu."))
            )
            2 -> AgentResponse(
                note = NoteDto(
                    title = "Nakupni seznam & Vikendovy plan",
                    summary = "Seznam nakupu a plan aktivit na vikend.",
                    markdown = "### Nakup\n* Chleba, maslo, syr\n* Rajcata, okurky, paprika\n* Kava a caj\n* Cistici prostredky\n\n### Vikendovy plan\n1. **Sobota rano**: Behani v parku (5 km)\n2. **Sobota odpoledne**: Vareni obeda, uprava bytu\n3. **Nedele**: Vylet na hrad + kafe s kamarady",
                    tags = listOf("Nakup", "Vikend", "Plan", "Osobni")
                ),
                todos = listOf(
                    TodoDto("todo-1", "Nakoupit potraviny", "high", tomorrow, false),
                    TodoDto("todo-2", "Rezervovat stul v restauraci", "medium", tomorrow, false)
                ),
                events = listOf(
                    EventDto("event-1", "Behani v parku", tomorrow, "08:00", "45m", "Ranni beh 5km v mestskem parku.", "Mestsky park")
                ),
                topics = listOf(TopicDto("Vikendovy plan", "Organizace volneho casu a nakupu."))
            )
            else -> AgentResponse(
                note = NoteDto(
                    title = "API Design Meeting Notes",
                    summary = "Key decisions from REST API architecture meeting.",
                    markdown = "### API Architecture\n* **Auth**: OAuth 2.0 with JWT refresh tokens\n* **Versioning**: URL-based (/v1/, /v2/)\n* **Rate limiting**: 100 req/min per user\n\n### Endpoints\n1. `POST /api/v1/notes` - Create note\n2. `GET /api/v1/notes/:id` - Get note\n3. `PUT /api/v1/notes/:id/enhance` - AI enhance\n4. `DELETE /api/v1/notes/:id` - Delete note\n\n### Database\n- PostgreSQL for structured data\n- Redis for caching & sessions",
                    tags = listOf("API", "Architecture", "Backend", "Meeting")
                ),
                todos = listOf(
                    TodoDto("todo-1", "Write OpenAPI spec for v1 endpoints", "high", tomorrow, false),
                    TodoDto("todo-2", "Set up Redis caching layer", "medium", nextWeek, false),
                    TodoDto("todo-3", "Review OAuth2 token flow with security team", "high", tomorrow, false)
                ),
                events = listOf(
                    EventDto("event-1", "API Security Review", nextWeek, "15:00", "1h", "Review OAuth2 implementation with security team.", "Google Meet")
                ),
                topics = listOf(TopicDto("REST API Design", "Architecture decisions for the InkAction backend API."))
            )
        }
        return AgentPipelineStatus.Success(mockResponse)
    }

    suspend fun enhanceNoteContent(originalNote: String, markdown: String, language: String): String? {
        if (apiKey.isBlank()) {
            return "Demo Enhancement: The provided note lacks details about X and Y. Consider elaborating on Z. Next steps could include validating the technical assumptions outlined."
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
