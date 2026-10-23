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
    val defaultEventTime: String,
    val fingerDrawingEnabled: Boolean = true,
    val cloudSyncEnabled: Boolean = false,
    val syncPassphrase: String = "",
    val syncAccountEmail: String = "",
    val lastSyncTimestamp: Long = 0L,
    val penStrokeWidth: Float = 3.5f,
    val firebaseProjectId: String = "",
    val firebaseApiKey: String = ""
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("inkaction_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        return AppSettings(
            apiKey = prefs.getString("api_key", "") ?: "",
            modelName = prefs.getString("model_name", "gemini-3.8-flash") ?: "gemini-3.8-flash",
            debounceDurationMs = prefs.getLong("debounce_ms", 6000L), // Default to 6 seconds for responsive auto-actionize
            remindersEnabled = prefs.getBoolean("reminders_enabled", false),
            noteLanguage = prefs.getString("note_language", "Auto-detect") ?: "Auto-detect",
            themeMode = prefs.getString("theme_mode", "system") ?: "system",
            defaultEventTime = prefs.getString("default_event_time", "08:00") ?: "08:00",
            fingerDrawingEnabled = prefs.getBoolean("finger_drawing_enabled", true),
            cloudSyncEnabled = prefs.getBoolean("cloud_sync_enabled", false),
            syncPassphrase = prefs.getString("sync_passphrase", "") ?: "",
            syncAccountEmail = prefs.getString("sync_account_email", "") ?: "",
            lastSyncTimestamp = prefs.getLong("last_sync_timestamp", 0L),
            penStrokeWidth = prefs.getFloat("pen_stroke_width", 3.5f),
            firebaseProjectId = prefs.getString("firebase_project_id", "") ?: "",
            firebaseApiKey = prefs.getString("firebase_api_key", "") ?: ""
        )
    }

    fun setFirebaseConfig(projectId: String, apiKey: String) {
        prefs.edit()
            .putString("firebase_project_id", projectId)
            .putString("firebase_api_key", apiKey)
            .apply()
        _settings.value = _settings.value.copy(
            firebaseProjectId = projectId,
            firebaseApiKey = apiKey
        )
    }

    fun setPenStrokeWidth(width: Float) {
        prefs.edit().putFloat("pen_stroke_width", width).apply()
        _settings.value = _settings.value.copy(penStrokeWidth = width)
    }

    fun setFingerDrawingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("finger_drawing_enabled", enabled).apply()
        _settings.value = _settings.value.copy(fingerDrawingEnabled = enabled)
    }

    fun setCloudSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("cloud_sync_enabled", enabled).apply()
        _settings.value = _settings.value.copy(cloudSyncEnabled = enabled)
    }

    fun setSyncPassphrase(passphrase: String) {
        prefs.edit().putString("sync_passphrase", passphrase).apply()
        _settings.value = _settings.value.copy(syncPassphrase = passphrase)
    }

    fun setSyncAccountEmail(email: String) {
        prefs.edit().putString("sync_account_email", email).apply()
        _settings.value = _settings.value.copy(syncAccountEmail = email)
    }

    fun setLastSyncTimestamp(timestamp: Long) {
        prefs.edit().putLong("last_sync_timestamp", timestamp).apply()
        _settings.value = _settings.value.copy(lastSyncTimestamp = timestamp)
    }

    fun setDebounceDurationMs(durationMs: Long) {
        prefs.edit().putLong("debounce_ms", durationMs).apply()
        _settings.value = _settings.value.copy(debounceDurationMs = durationMs)
    }

    fun saveSettings(
        apiKey: String,
        modelName: String,
        remindersEnabled: Boolean,
        noteLanguage: String,
        themeMode: String,
        defaultEventTime: String,
        cloudSyncEnabled: Boolean = false,
        syncPassphrase: String = "",
        debounceDurationMs: Long = _settings.value.debounceDurationMs
    ) {
        prefs.edit()
            .putString("api_key", apiKey)
            .putString("model_name", modelName)
            .putLong("debounce_ms", debounceDurationMs)
            .putBoolean("reminders_enabled", remindersEnabled)
            .putString("note_language", noteLanguage)
            .putString("theme_mode", themeMode)
            .putString("default_event_time", defaultEventTime)
            .putBoolean("cloud_sync_enabled", cloudSyncEnabled)
            .putString("sync_passphrase", syncPassphrase)
            .apply()
        
        _settings.value = loadSettings()
    }
}
