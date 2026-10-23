@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.inkaction.app.ui.todos

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkaction.app.R
import com.inkaction.app.data.model.SavedTodo
import com.inkaction.app.ui.theme.PriorityHighColor
import com.inkaction.app.ui.theme.PriorityLowColor
import com.inkaction.app.ui.theme.PriorityMediumColor

@Composable
fun TodosScreen(
    todos: List<SavedTodo>,
    onToggleTodo: (String, Boolean) -> Unit,
    onDeleteTodo: (String) -> Unit,
    onNavigateToNote: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (todos.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(24.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(28.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.todos_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.todos_empty_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    val haptics = LocalHapticFeedback.current
    val recentlyCompleted = remember { mutableStateListOf<String>() }
    val unarchivedTodos = todos.filter { !it.isArchived }
    val activeTodos = unarchivedTodos.filter { !it.isCompleted || recentlyCompleted.contains(it.id) }.sortedByDescending { it.timestamp }
    val completedTodos = unarchivedTodos.filter { it.isCompleted && !recentlyCompleted.contains(it.id) }.sortedByDescending { it.timestamp }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (activeTodos.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.todos_active_header) + " (${activeTodos.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            items(activeTodos, key = { it.id.ifBlank { "${it.timestamp}_${it.text.hashCode()}" } }) { todo ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = {
                        if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDeleteTodo(todo.id)
                            true
                        } else false
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(18.dp))
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                Icons.Default.Archive,
                                contentDescription = stringResource(R.string.archive_title),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                ) {
                    TodoCard(
                        todo = todo,
                        onToggle = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            val newStatus = !todo.isCompleted
                            onToggleTodo(todo.id, newStatus)
                            if (newStatus) {
                                recentlyCompleted.add(todo.id)
                            } else {
                                recentlyCompleted.remove(todo.id)
                            }
                        },
                        onDelete = { onDeleteTodo(todo.id) },
                        onNavigateToNote = if (todo.noteId != null) { { onNavigateToNote(todo.noteId) } } else null
                    )
                }
            }
        }

        if (completedTodos.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.todos_completed_header) + " (${completedTodos.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(completedTodos, key = { "${it.id}_completed" }) { todo ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = {
                        if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDeleteTodo(todo.id)
                            true
                        } else false
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(18.dp))
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                Icons.Default.Archive,
                                contentDescription = stringResource(R.string.archive_title),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                ) {
                    TodoCard(
                        todo = todo,
                        onToggle = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            val newStatus = !todo.isCompleted
                            onToggleTodo(todo.id, newStatus)
                            if (newStatus) {
                                recentlyCompleted.add(todo.id)
                            } else {
                                recentlyCompleted.remove(todo.id)
                            }
                        },
                        onDelete = { onDeleteTodo(todo.id) },
                        onNavigateToNote = if (todo.noteId != null) { { onNavigateToNote(todo.noteId) } } else null
                    )
                }
            }
        }
    }
}

@Composable
fun TodoCard(
    todo: SavedTodo,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onNavigateToNote: (() -> Unit)? = null
) {
    val isDone = todo.isCompleted
    val priorityColor = when (todo.priority.lowercase()) {
        "high" -> PriorityHighColor
        "low" -> PriorityLowColor
        else -> PriorityMediumColor
    }

    val animatedTextColor by animateColorAsState(
        targetValue = if (isDone) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 250),
        label = "color"
    )

    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Elegant circular checkbox
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isDone) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .border(
                        width = 2.dp,
                        color = if (isDone) MaterialTheme.colorScheme.primary else priorityColor,
                        shape = CircleShape
                    )
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Task content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(if (onNavigateToNote != null) Modifier.clickable { onNavigateToNote() } else Modifier)
            ) {
                Text(
                    text = todo.text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isDone) FontWeight.Normal else FontWeight.Medium,
                    color = animatedTextColor,
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                )

                // Badges row: Priority, Due Date, Note link
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Priority chip
                    val priorityLabel = when (todo.priority.lowercase()) {
                        "high" -> stringResource(R.string.priority_high)
                        "low" -> stringResource(R.string.priority_low)
                        else -> stringResource(R.string.priority_medium)
                    }

                    Surface(
                        shape = CircleShape,
                        color = priorityColor.copy(alpha = if (isDone) 0.1f else 0.18f)
                    ) {
                        Text(
                            text = priorityLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDone) MaterialTheme.colorScheme.outline else priorityColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    // Due date chip
                    if (todo.dueDate.isNotBlank() && todo.dueDate != "None") {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = if (isDone) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = todo.dueDate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDone) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Note link chip
                    if (onNavigateToNote != null) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = stringResource(R.string.checkbox_note),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }

            // Quick Delete Button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
