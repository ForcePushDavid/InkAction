package com.inkaction.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.inkaction.app.ui.theme.AccentBlue
import com.inkaction.app.ui.theme.BgSurface
import com.inkaction.app.ui.theme.TextMuted
import com.inkaction.app.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    initialApiKey: String,
    initialModel: String,
    initialDebounceMs: Long,
    onDismiss: () -> Unit,
    onSave: (apiKey: String, model: String, debounceMs: Long) -> Unit
) {
    var apiKey by remember { mutableStateOf(initialApiKey) }
    var model by remember { mutableStateOf(initialModel.ifBlank { "gemini-3.7-flash" }) }
    var debounceMs by remember { mutableStateOf(initialDebounceMs) }
    var modelExpanded by remember { mutableStateOf(false) }

    val presetModels = listOf(
        "gemini-3.7-flash" to "Gemini 3.7 Flash (Flagship SOTA - Recommended)",
        "gemini-3.6-flash" to "Gemini 3.6 Flash (High-Speed Multimodal)",
        "gemini-3.5-flash" to "Gemini 3.5 Flash",
        "gemini-3.5-flash-lite" to "Gemini 3.5 Flash Lite (Ultra Low Latency)",
        "gemini-3.1-pro" to "Gemini 3.1 Pro (Deep Vision & Diagram Analysis)",
        "gemini-3.1-flash-lite" to "Gemini 3.1 Flash Lite",
        "gemini-3-flash" to "Gemini 3 Flash",
        "gemini-2.5-flash" to "Gemini 2.5 Flash",
        "gemini-2.5-flash-lite" to "Gemini 2.5 Flash Lite",
        "gemini-2.5-pro" to "Gemini 2.5 Pro",
        "gemini-2-flash" to "Gemini 2 Flash",
        "gemini-2-flash-lite" to "Gemini 2 Flash Lite"
    )

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BgSurface)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "InkAction Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("Google Gemini API Key") },
                placeholder = { Text("AIzaSy...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text(
                text = "Leave blank for instant Smart Demo mode.",
                fontSize = 11.sp,
                color = TextMuted
            )

            // Editable Model Selection + Dropdown
            ExposedDropdownMenuBox(
                expanded = modelExpanded,
                onExpandedChange = { modelExpanded = !modelExpanded }
            ) {
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Gemini Model (Type or Select)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = modelExpanded,
                    onDismissRequest = { modelExpanded = false }
                ) {
                    presetModels.forEach { (modelId, description) ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(text = modelId, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = description, fontSize = 11.sp, color = TextMuted)
                                }
                            },
                            onClick = {
                                model = modelId
                                modelExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextMuted)
                }
                Button(
                    onClick = { onSave(apiKey, model, debounceMs) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Save & Apply")
                }
            }
        }
    }
}
