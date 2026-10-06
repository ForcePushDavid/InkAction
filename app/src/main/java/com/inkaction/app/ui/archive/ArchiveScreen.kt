package com.inkaction.app.ui.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.inkaction.app.R
import com.inkaction.app.data.model.SavedNote
import com.inkaction.app.data.model.SavedTodo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveDialog(
    archivedNotes: List<SavedNote>,
    archivedTodos: List<SavedTodo>,
    onRestoreNote: (Long) -> Unit,
    onDeleteNotePermanently: (Long) -> Unit,
    onRestoreTodo: (String) -> Unit,
    onDeleteTodoPermanently: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var noteToDelete by remember { mutableStateOf<SavedNote?>(null) }
    var todoToDelete by remember { mutableStateOf<SavedTodo?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.archive_title)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("${stringResource(R.string.tab_notes)} (${archivedNotes.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("${stringResource(R.string.tab_todos)} (${archivedTodos.size})") }
                    )
                }

                if (selectedTab == 0) {
                    if (archivedNotes.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.archive_empty_notes),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(archivedNotes, key = { it.id }) { note ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = note.title.ifBlank { stringResource(R.string.untitled_note) },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (note.summary.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = note.summary,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 2
                                                )
                                            }
                                        }
                                        IconButton(onClick = { onRestoreNote(note.id) }) {
                                            Icon(
                                                Icons.Default.Restore,
                                                contentDescription = stringResource(R.string.restore),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = { noteToDelete = note }) {
                                            Icon(
                                                Icons.Default.DeleteForever,
                                                contentDescription = stringResource(R.string.delete_permanently),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (archivedTodos.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.archive_empty_todos),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(archivedTodos, key = { it.id }) { todo ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = todo.text,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (todo.dueDate.isNotBlank() && todo.dueDate != "None") {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = todo.dueDate,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                        IconButton(onClick = { onRestoreTodo(todo.id) }) {
                                            Icon(
                                                Icons.Default.Restore,
                                                contentDescription = stringResource(R.string.restore),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = { todoToDelete = todo }) {
                                            Icon(
                                                Icons.Default.DeleteForever,
                                                contentDescription = stringResource(R.string.delete_permanently),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        noteToDelete?.let { note ->
            AlertDialog(
                onDismissRequest = { noteToDelete = null },
                title = { Text(stringResource(R.string.delete_permanently_title)) },
                text = { Text(stringResource(R.string.delete_permanently_note_msg, note.title)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteNotePermanently(note.id)
                            noteToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.delete_permanently))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { noteToDelete = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        todoToDelete?.let { todo ->
            AlertDialog(
                onDismissRequest = { todoToDelete = null },
                title = { Text(stringResource(R.string.delete_permanently_title)) },
                text = { Text(stringResource(R.string.delete_permanently_todo_msg, todo.text)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteTodoPermanently(todo.id)
                            todoToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.delete_permanently))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { todoToDelete = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}
