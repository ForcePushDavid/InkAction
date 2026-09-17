package com.inkaction.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.foundation.layout.size
import com.inkaction.app.ui.theme.AccentGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.HttpURLConnection
import java.io.InputStreamReader
import com.google.gson.JsonParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    initialApiKey: String,
    initialModel: String,
    initialDebounceMs: Long,
    initialReminders: Boolean,
    initialLanguage: String,
    initialThemeMode: String,
    initialDefaultEventTime: String,
    onDismiss: () -> Unit,
    onSave: (apiKey: String, model: String, debounceMs: Long, reminders: Boolean, language: String, themeMode: String, defaultEventTime: String) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    var apiKey by remember { mutableStateOf(initialApiKey) }
    var model by remember { mutableStateOf(initialModel.ifBlank { "gemini-3.5-flash-lite" }) }
    var debounceMs by remember { mutableStateOf(initialDebounceMs) }
    var remindersEnabled by remember { mutableStateOf(initialReminders) }
    var language by remember { mutableStateOf(initialLanguage) }
    var themeMode by remember { mutableStateOf(initialThemeMode) }
    var defaultEventTime by remember { mutableStateOf(initialDefaultEventTime) }
    var modelExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }

    // Výchozí modely, pokud selže stahování nebo klíč není zadán
    val defaultModels = listOf(
        Triple("gemini-3.8-flash", "Gemini 3.8 Flash (Výchozí)", "🚀 Výchozí standardní model"),
        Triple("gemini-3.5-flash", "Gemini 3.5 Flash", "⚡ Starší rychlý model")
    )
    var activeModels by remember { mutableStateOf(defaultModels) }
    var isFetchingModels by remember { mutableStateOf(false) }

    LaunchedEffect(apiKey) {
        if (apiKey.isNotBlank() && apiKey.length > 10) {
            isFetchingModels = true
            val fetchedModels = withContext(Dispatchers.IO) {
                try {
                    val url = URL("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    val reader = InputStreamReader(conn.inputStream)
                    val jsonObject = JsonParser.parseReader(reader).asJsonObject
                    val modelsArray = jsonObject.getAsJsonArray("models")
                    
                    val result = mutableListOf<Triple<String, String, String>>()
                    for (item in modelsArray) {
                        val m = item.asJsonObject
                        val name = m.get("name").asString.removePrefix("models/")
                        if (!name.startsWith("gemini")) continue
                        
                        val methods = m.getAsJsonArray("supportedGenerationMethods")
                        val supportsGeneration = methods.map { it.asString }.contains("generateContent")
                        if (!supportsGeneration) continue
                        
                        val displayName = if (m.has("displayName")) m.get("displayName").asString else name
                        val description = if (m.has("description")) m.get("description").asString else ""
                        result.add(Triple(name, displayName, description))
                    }
                    if (result.isNotEmpty()) result else defaultModels
                } catch (e: Exception) {
                    defaultModels
                }
            }
            activeModels = fetchedModels
            isFetchingModels = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = "InkAction Nastavení",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "v" + com.inkaction.app.BuildConfig.VERSION_NAME,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
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
                color = MaterialTheme.colorScheme.outline
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
                    trailingIcon = { 
                        if (isFetchingModels) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) 
                        }
                    },
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
                                        Text(text = name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
                                        Text(text = modelId, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    Text(text = quota, fontSize = 11.sp, color = if (modelId.contains("lite")) AccentGreen else MaterialTheme.colorScheme.outline)
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
            Text(text = "Vzhled aplikace", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("system" to "Systém", "dark" to "Tmavý", "light" to "Světlý").forEach { (value, label) ->
                    Button(
                        onClick = { themeMode = value },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (themeMode == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (themeMode == value) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onBackground
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (themeMode == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
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
                    Text("Povolit připomínky", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Lokální notifikace pro úkoly a události", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                }
                androidx.compose.material3.Switch(
                    checked = remindersEnabled,
                    onCheckedChange = { remindersEnabled = it }
                )
            }

            OutlinedTextField(
                value = defaultEventTime,
                onValueChange = { defaultEventTime = it },
                label = { Text("Výchozí čas událostí (HH:MM)") },
                placeholder = { Text("08:00") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text(
                text = "Tento čas se použije, pokud AI nenajde v textu přesnou hodinu.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Záloha a obnova", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onExportBackup, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onBackground)) {
                    Text("Exportovat (ZIP)", fontSize = 12.sp)
                }
                Button(onClick = onImportBackup, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onBackground)) {
                    Text("Importovat (ZIP)", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Zrušit", color = MaterialTheme.colorScheme.outline)
                }
                Button(
                    onClick = { onSave(apiKey, model, debounceMs, remindersEnabled, language, themeMode, defaultEventTime) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Uložit a použít")
                }
            }
        }
    }
}
