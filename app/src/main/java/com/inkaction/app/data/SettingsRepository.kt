package com.inkaction.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val apiKey: String,
    val modelName: String,
    val debounceDurationMs: Long,
    val remindersEnabled: Boolean,
    val noteLanguage: String,
    val themeMode: String,
    val defaultEventTime: String
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("inkaction_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        return AppSettings(
            apiKey = prefs.getString("api_key", "") ?: "",
            modelName = prefs.getString("model_name", "gemini-3.8-flash") ?: "gemini-3.8-flash",
            debounceDurationMs = 600000L, // Fixed to 10 mins as per earlier implementation
            remindersEnabled = prefs.getBoolean("reminders_enabled", false),
            noteLanguage = prefs.getString("note_language", "Auto-detect") ?: "Auto-detect",
            themeMode = prefs.getString("theme_mode", "system") ?: "system",
            defaultEventTime = prefs.getString("default_event_time", "08:00") ?: "08:00"
        )
    }

    fun saveSettings(
        apiKey: String,
        modelName: String,
        remindersEnabled: Boolean,
        noteLanguage: String,
        themeMode: String,
        defaultEventTime: String
    ) {
        prefs.edit()
            .putString("api_key", apiKey)
            .putString("model_name", modelName)
            .putLong("debounce_ms", 600000L)
            .putBoolean("reminders_enabled", remindersEnabled)
            .putString("note_language", noteLanguage)
            .putString("theme_mode", themeMode)
            .putString("default_event_time", defaultEventTime)
            .apply()
        
        _settings.value = loadSettings()
    }
}
