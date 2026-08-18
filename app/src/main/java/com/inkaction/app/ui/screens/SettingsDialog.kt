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
import com.inkaction.app.ui.theme.AccentCyan
import com.inkaction.app.ui.theme.AccentGreen
import com.inkaction.app.ui.theme.BgSurface
import com.inkaction.app.ui.theme.TextMuted
import com.inkaction.app.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    initialApiKey: String,
    initialModel: String,
    initialDebounceMs: Long,
    initialReminders: Boolean,
    initialLanguage: String,
    initialThemeMode: String,
    onDismiss: () -> Unit,
    onSave: (apiKey: String, model: String, debounceMs: Long, reminders: Boolean, language: String, themeMode: String) -> Unit
) {
    var apiKey by remember { mutableStateOf(initialApiKey) }
    var model by remember { mutableStateOf(initialModel.ifBlank { "gemini-3.5-flash-lite" }) }
    var debounceMs by remember { mutableStateOf(initialDebounceMs) }
    var remindersEnabled by remember { mutableStateOf(initialReminders) }
    var language by remember { mutableStateOf(initialLanguage) }
    var themeMode by remember { mutableStateOf(initialThemeMode) }
    var modelExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }

    // Seznam modelů s přesným rozepsáním RPM (dotazy za minutu) a RPD (dotazy za den)
    val activeModels = listOf(
        Triple("gemini-3.5-flash-lite", "Gemini 3.5 Flash Lite (Výchozí)", "🚀 15 RPM / 500 RPD | Rychlý, pro běžné psaní"),
        Triple("gemini-3.7-flash", "Gemini 3.7 Flash", "🧠 5 RPM / 20 RPD | Nejchytřejší, bacha na limit"),
        Triple("gemini-3.6-flash", "Gemini 3.6 Flash", "⚡ 5 RPM / 20 RPD | Multimodální standard"),
        Triple("gemini-3.5-flash", "Gemini 3.5 Flash", "⚡ 5 RPM / 20 RPD | Starší standard")
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
                text = "InkAction Nastavení",
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
                text = "Ponechte prázdné pro lokální Smart Demo režim.",
                fontSize = 11.sp,
                color = TextMuted
            )

            // Dropdown containing strictly active models with quota labels
            ExposedDropdownMenuBox(
                expanded = modelExpanded,
                onExpandedChange = { modelExpanded = !modelExpanded }
            ) {
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Aktivní Gemini Model") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = modelExpanded,
                    onDismissRequest = { modelExpanded = false }
                ) {
                    activeModels.forEach { (modelId, name, quota) ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                        Text(text = modelId, fontSize = 11.sp, color = AccentCyan)
                                    }
                                    Text(text = quota, fontSize = 11.sp, color = if (modelId.contains("lite")) AccentGreen else TextMuted)
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

            Spacer(modifier = Modifier.height(16.dp))

            val languages = listOf("Auto-detect", "Čeština", "Slovenčina", "English", "Deutsch", "Español", "Français", "Italiano", "Polski", "Русский", "Українська")

            ExposedDropdownMenuBox(
                expanded = languageExpanded,
                onExpandedChange = { languageExpanded = it }
            ) {
                OutlinedTextField(
                    value = language,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Jazyk poznámky") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = languageExpanded,
                    onDismissRequest = { languageExpanded = false }
                ) {
                    languages.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(text = lang) },
                            onClick = {
                                language = lang
                                languageExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Theme selection
            Text(text = "Vzhled aplikace", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("system" to "Systém", "dark" to "Tmavý", "light" to "Světlý").forEach { (value, label) ->
                    Button(
                        onClick = { themeMode = value },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (themeMode == value) AccentBlue else BgSurface,
                            contentColor = if (themeMode == value) androidx.compose.ui.graphics.Color.White else TextPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (themeMode == value) AccentBlue else com.inkaction.app.ui.theme.BorderColor)
                    ) {
                        Text(label, fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column {
                    Text("Povolit připomínky", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Lokální notifikace pro úkoly a události", color = TextMuted, fontSize = 12.sp)
                }
                androidx.compose.material3.Switch(
                    checked = remindersEnabled,
                    onCheckedChange = { remindersEnabled = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Zrušit", color = TextMuted)
                }
                Button(
                    onClick = { onSave(apiKey, model, debounceMs, remindersEnabled, language, themeMode) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Uložit a použít")
                }
            }
        }
    }
}
