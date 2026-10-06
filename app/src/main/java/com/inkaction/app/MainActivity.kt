package com.inkaction.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.inkaction.app.ui.workspace.MainWorkspaceScreen
import com.inkaction.app.ui.theme.InkActionTheme
import com.inkaction.app.viewmodel.InkActionViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: InkActionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val settings by viewModel.settings.collectAsState()
            val isSystemDark = isSystemInDarkTheme()
            val isDarkTheme = when (settings.themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemDark
            }
            
            InkActionTheme(isDarkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainWorkspaceScreen(viewModel = viewModel)
                }
            }
        }
    }
}
