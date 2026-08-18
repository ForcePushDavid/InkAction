package com.inkaction.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.inkaction.app.ui.screens.MainWorkspaceScreen
import com.inkaction.app.ui.theme.BgDark
import com.inkaction.app.ui.theme.InkActionTheme
import com.inkaction.app.viewmodel.InkActionViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: InkActionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            InkActionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDark
                ) {
                    MainWorkspaceScreen(viewModel = viewModel)
                }
            }
        }
    }
}
