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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
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
import com.inkaction.app.ai.TodoDto
import com.inkaction.app.ui.theme.AccentAmber
import com.inkaction.app.ui.theme.AccentBlue
import com.inkaction.app.ui.theme.AccentGreen
import com.inkaction.app.ui.theme.AccentRed
import com.inkaction.app.ui.theme.BgSurface
import com.inkaction.app.ui.theme.BorderColor
import com.inkaction.app.ui.theme.TextMuted
import com.inkaction.app.ui.theme.TextPrimary
import com.inkaction.app.ui.theme.TextSecondary

@Composable
fun TodosScreen(
    todos: List<TodoDto>,
    onToggleTodo: (String) -> Unit,
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
                    tint = TextMuted,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "No pending action items",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Checklists, tasks, and follow-ups detected in your handwriting appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(todos, key = { it.id }) { todo ->
            TodoCard(todo = todo, onToggle = { onToggleTodo(todo.id) })
        }
    }
}

@Composable
fun TodoCard(
    todo: TodoDto,
    onToggle: () -> Unit
) {
    val priorityColor = when (todo.priority.lowercase()) {
        "high" -> AccentRed
        "low" -> AccentBlue
        else -> AccentAmber
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (todo.completed) BgSurface.copy(alpha = 0.3f) else BgSurface)
            .androidx.compose.foundation.clickable { onToggle() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Todoist style circular checkbox colored by priority
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .androidx.compose.foundation.border(
                    width = 2.dp, 
                    color = if (todo.completed) TextMuted else priorityColor, 
                    shape = androidx.compose.foundation.shape.CircleShape
                )
                .background(if (todo.completed) TextMuted else androidx.compose.ui.graphics.Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (todo.completed) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Check,
                    contentDescription = null,
                    tint = BgSurface,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = todo.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (todo.completed) TextMuted else TextPrimary,
                textDecoration = if (todo.completed) TextDecoration.LineThrough else TextDecoration.None
            )

            if (todo.dueDate.isNotBlank() && todo.dueDate != "None") {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = AccentRed,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = todo.dueDate,
                        fontSize = 12.sp,
                        color = AccentRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
