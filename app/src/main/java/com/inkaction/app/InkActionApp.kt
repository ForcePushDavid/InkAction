package com.inkaction.app

import android.app.Application
import com.inkaction.app.data.NoteRepository
import com.inkaction.app.data.SettingsRepository

class InkActionApp : Application() {
    lateinit var noteRepository: NoteRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        noteRepository = NoteRepository(this)
        settingsRepository = SettingsRepository(this)
    }
}
