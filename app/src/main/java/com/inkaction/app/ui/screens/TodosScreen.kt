package com.inkaction.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkaction.app.data.SavedTodo
import com.inkaction.app.ui.theme.AccentAmber
import com.inkaction.app.ui.theme.AccentRed

@Composable
fun TodosScreen(
    todos: List<SavedTodo>,
    onToggleTodo: (String, Boolean) -> Unit,
    onDeleteTodo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (todos.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Checklist,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Žádné úkoly",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        return
    }

    val activeTodos = todos.filter { !it.isCompleted }.sortedByDescending { it.timestamp }
    val completedTodos = todos.filter { it.isCompleted }.sortedByDescending { it.timestamp }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (activeTodos.isNotEmpty()) {
            item {
                Text(
                    text = "Aktivní úkoly",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(activeTodos, key = { it.id.ifBlank { "${it.timestamp}_${it.text.hashCode()}" } }) { todo ->
                TodoCard(
                    todo = todo, 
                    onToggle = { onToggleTodo(todo.id, todo.isCompleted) },
                    onDelete = { onDeleteTodo(todo.id) }
                )
            }
        }

        if (completedTodos.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Historie (Dokončené)",
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(completedTodos, key = { "${it.id}_completed" }) { todo ->
                TodoCard(
                    todo = todo, 
                    onToggle = { onToggleTodo(todo.id, todo.isCompleted) },
                    onDelete = { onDeleteTodo(todo.id) }
                )
            }
        }
    }
}

@Composable
fun TodoCard(
    todo: SavedTodo,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isDone = todo.isCompleted
    val priorityColor = when (todo.priority.lowercase()) {
        "high" -> AccentRed
        "low" -> MaterialTheme.colorScheme.primary
        else -> AccentAmber
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDone) MaterialTheme.colorScheme.surface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface)
            .clickable { onToggle() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Todoist style circular checkbox colored by priority
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .border(
                    width = 2.dp, 
                    color = if (isDone) MaterialTheme.colorScheme.outline else priorityColor, 
                    shape = androidx.compose.foundation.shape.CircleShape
                )
                .background(if (isDone) MaterialTheme.colorScheme.outline else androidx.compose.ui.graphics.Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = todo.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDone) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onBackground,
                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
            )

            if (todo.dueDate.isNotBlank() && todo.dueDate != "None") {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = if (isDone) MaterialTheme.colorScheme.outline else AccentRed,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = todo.dueDate,
                        fontSize = 12.sp,
                        color = if (isDone) MaterialTheme.colorScheme.outline else AccentRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Smazat",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}