package com.inkaction.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkaction.app.ai.NoteDto
import com.inkaction.app.data.SavedNote
import com.inkaction.app.ui.theme.AccentBlue
import com.inkaction.app.ui.theme.AccentCyan
import com.inkaction.app.ui.theme.AccentPurple
import com.inkaction.app.ui.theme.BgSurface
import com.inkaction.app.ui.theme.BgTertiary
import com.inkaction.app.ui.theme.TextMuted
import com.inkaction.app.ui.theme.TextPrimary
import com.inkaction.app.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotesScreen(
    note: NoteDto?,
    allNotes: List<SavedNote>,
    enhancingNotes: Map<Long, Boolean> = emptyMap(),
    folders: List<com.inkaction.app.data.NoteFolder>,
    onCreateFolder: (String, String) -> Unit,
    onMoveNote: (Long, Long?) -> Unit,
    onResumeDrawing: (SavedNote) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onTogglePin: (Long) -> Unit,
    onEnhanceNote: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }
    var newFolderName by remember { mutableStateOf("") }
    
    val allTags = allNotes.flatMap { it.tags }.distinct().sorted()
    
    val filteredNotes = allNotes.filter { savedNote ->
        val matchesSearch = searchQuery.isBlank() || 
            savedNote.title.contains(searchQuery, ignoreCase = true) ||
            savedNote.summary.contains(searchQuery, ignoreCase = true) ||
            savedNote.markdown.contains(searchQuery, ignoreCase = true) ||
            savedNote.tags.any { it.contains(searchQuery, ignoreCase = true) }
            
        val matchesTag = selectedTag == null || savedNote.tags.contains(selectedTag)
        val matchesFolder = selectedFolderId == null || savedNote.folderId == selectedFolderId
        
        matchesSearch && matchesTag && matchesFolder
    }.sortedWith(compareByDescending<SavedNote> { it.isPinned }.thenByDescending { it.timestamp })

    if (showCreateFolderDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Nová složka") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Název složky") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newFolderName.isNotBlank()) {
                        onCreateFolder(newFolderName, "#38BDF8")
                        showCreateFolderDialog = false
                        newFolderName = ""
                    }
                }) {
                    Text("Vytvořit")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Zrušit")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (note != null) {
            Text("Aktivní poznámka", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            NoteCard(note = note, context = context)
        } 
        
        if (allNotes.isEmpty()) {
            if (note == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Knihovna poznámek je prázdná",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                }
            }
        } else {
            Text("Historie (Knihovna)", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Hledat poznámky...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                singleLine = true
            )
            
            // Složky
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AccentBlue)
                        .clickable { showCreateFolderDialog = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("+ Složka", color = androidx.compose.ui.graphics.Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                folders.forEach { folder ->
                    val isSelected = folder.id == selectedFolderId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) AccentCyan else BgTertiary)
                            .clickable {
                                selectedFolderId = if (isSelected) null else folder.id
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = folder.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) BgSurface else TextPrimary
                        )
                    }
                }
            }

            if (allTags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allTags.forEach { tag ->
                        val isSelected = tag == selectedTag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) AccentPurple else BgTertiary)
                                .clickable { 
                                    if (isSelected) selectedTag = null else selectedTag = tag
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) BgSurface else AccentPurple
                            )
                        }
                    }
                }
            }
            
            filteredNotes.forEach { savedNote ->
                SavedNoteCard(
                    savedNote = savedNote, 
                    context = context, 
                    folders = folders,
                    onMoveNote = { folderId -> onMoveNote(savedNote.id, folderId) },
                    onResumeDrawing = { onResumeDrawing(savedNote) },
                    onDeleteNote = { onDeleteNote(savedNote.id) },
                    onTogglePin = { onTogglePin(savedNote.id) },
                    onEnhance = { onEnhanceNote(savedNote.id) },
                    isEnhancing = enhancingNotes[savedNote.id] == true
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteCard(note: NoteDto, context: Context) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgSurface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title.ifBlank { "Nová poznámka" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                val sdf = java.text.SimpleDateFormat("dd. MM. yyyy, HH:mm", java.util.Locale.getDefault())
                val dateString = sdf.format(java.util.Date(note.timestamp))

                Text(
                    text = "Syntetizováno: $dateString",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            Row {
                IconButton(
                    onClick = {
                        com.inkaction.app.util.PdfExportUtil.exportNoteToPdf(
                            context,
                            note.title,
                            "${note.summary}\n\n${note.markdown}"
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "Export to PDF",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Synthesized Note", "# ${note.title}\n\n${note.summary}\n\n${note.markdown}")
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Note copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Note",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (note.summary.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentCyan.copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Text(
                    text = note.summary,
                    fontSize = 13.sp,
                    color = AccentCyan,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = note.markdown,
            fontSize = 14.sp,
            color = TextSecondary,
            lineHeight = 22.sp
        )

        if (note.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                note.tags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(BgTertiary)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "#$tag",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentPurple
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SavedNoteCard(
    savedNote: SavedNote, 
    context: Context, 
    folders: List<com.inkaction.app.data.NoteFolder>,
    onMoveNote: (Long?) -> Unit,
    onResumeDrawing: () -> Unit, 
    onDeleteNote: () -> Unit,
    onTogglePin: () -> Unit,
    onEnhance: () -> Unit,
    isEnhancing: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgSurface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = savedNote.title.ifBlank { "Nová poznámka" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    
                    if (savedNote.folderId != null) {
                        val folder = folders.find { it.id == savedNote.folderId }
                        if (folder != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(folder.colorHex)))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(folder.name, fontSize = 10.sp, color = BgSurface, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                val sdf = java.text.SimpleDateFormat("dd. MM. yyyy, HH:mm", java.util.Locale.getDefault())
                val dateString = sdf.format(java.util.Date(savedNote.timestamp))

                Text(
                    text = "Syntetizováno: $dateString",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Folder Dropdown
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.EditNote, contentDescription = "Přiřadit do složky", tint = TextMuted)
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Bez složky") },
                            onClick = { onMoveNote(null); expanded = false }
                        )
                        folders.forEach { folder ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(folder.name) },
                                onClick = { onMoveNote(folder.id); expanded = false }
                            )
                        }
                    }
                }
                
                if (savedNote.aiEnhancement == null) {
                    if (isEnhancing) {
                        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(24.dp), color = AccentPurple)
                    } else {
                        androidx.compose.material3.TextButton(onClick = onEnhance) {
                            Text("✨ AI Enhance", color = AccentPurple)
                        }
                    }
                }
                IconButton(onClick = onTogglePin, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (savedNote.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin, 
                        contentDescription = if (savedNote.isPinned) "Odepnout" else "Připnout", 
                        tint = if (savedNote.isPinned) AccentPurple else TextMuted
                    )
                }
                IconButton(onClick = onDeleteNote, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Smazat", tint = TextMuted)
                }
                Button(
                    onClick = onResumeDrawing,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Kreslit")
                }
            }
        }

        if (savedNote.summary.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentCyan.copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Text(
                    text = savedNote.summary,
                    fontSize = 13.sp,
                    color = AccentCyan,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = savedNote.markdown,
            fontSize = 14.sp,
            color = TextSecondary,
            lineHeight = 22.sp
        )

        if (savedNote.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                savedNote.tags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(BgTertiary)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "#$tag",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentPurple
                        )
                    }
                }
            }
        }

        if (savedNote.aiEnhancement != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentPurple.copy(alpha = 0.1f))
                    .border(1.dp, AccentPurple, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text("✨ AI Insight", color = AccentPurple, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = savedNote.aiEnhancement,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
