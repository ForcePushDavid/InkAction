package com.inkaction.app.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.inkaction.app.ui.canvas.InkCanvasView
import com.inkaction.app.ui.canvas.ToolType
import com.inkaction.app.ui.components.AutoPushCountdownPill
import com.inkaction.app.ui.theme.AccentBlue
import com.inkaction.app.ui.theme.AccentCyan
import com.inkaction.app.ui.theme.BgDark
import com.inkaction.app.ui.theme.BgSurface
import com.inkaction.app.ui.theme.BgTertiary
import com.inkaction.app.ui.theme.BorderColor
import com.inkaction.app.ui.theme.TextMuted
import com.inkaction.app.ui.theme.TextPrimary
import com.inkaction.app.ui.theme.TextSecondary
import com.inkaction.app.viewmodel.InkActionViewModel

@Composable
fun MainWorkspaceScreen(
    viewModel: InkActionViewModel,
    modifier: Modifier = Modifier
) {
    var inkCanvasRef by remember { mutableStateOf<InkCanvasView?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    val autoPushState by viewModel.autoPushState.collectAsState()
    val currentTool by viewModel.currentTool.collectAsState()
    val currentColor by viewModel.currentColor.collectAsState()

    val currentNote by viewModel.currentNote.collectAsState()
    val todos by viewModel.todos.collectAsState()
    val events by viewModel.events.collectAsState()

    var selectedActionTab by remember { mutableIntStateOf(0) }
    var phoneNavTab by remember { mutableIntStateOf(0) } // 0 = Canvas, 1 = Actions

    if (showSettings) {
        SettingsDialog(
            initialApiKey = viewModel.apiKey,
            initialModel = viewModel.modelName,
            initialDebounceMs = viewModel.debounceDurationMs,
            onDismiss = { showSettings = false },
            onSave = { key, model, debounce ->
                viewModel.saveSettings(key, model, debounce)
                showSettings = false
            }
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(BgDark)) {
        val isTabletLayout = maxWidth >= 700.dp // Galaxy Tab S9 vs Galaxy S26 Ultra

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(BgSurface)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🖋️", fontSize = 16.sp)
                    }
                    Text(
                        text = "InkAction",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isTabletLayout) "Galaxy Tab S9" else "S26 Ultra",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentCyan
                        )
                    }
                }

                IconButton(onClick = { showSettings = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary
                    )
                }
            }

            // Workspace Content
            if (isTabletLayout) {
                // Dual-Pane Layout for Galaxy Tab S9
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    // Left Canvas Pane
                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                            .border(1.dp, BorderColor)
                    ) {
                        CanvasPaneContent(
                            viewModel = viewModel,
                            currentTool = currentTool,
                            currentColor = currentColor,
                            autoPushState = autoPushState,
                            onCanvasCreated = { inkCanvasRef = it }
                        )
                    }

                    // Right Actions Pane
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(BgDark)
                    ) {
                        ActionsPaneContent(
                            selectedTab = selectedActionTab,
                            onTabSelected = { selectedActionTab = it },
                            note = currentNote,
                            todos = todos,
                            events = events,
                            onToggleTodo = { viewModel.toggleTodo(it) }
                        )
                    }
                }
            } else {
                // Mobile Navigation Layout for Galaxy S26 Ultra
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (phoneNavTab == 0) {
                        CanvasPaneContent(
                            viewModel = viewModel,
                            currentTool = currentTool,
                            currentColor = currentColor,
                            autoPushState = autoPushState,
                            onCanvasCreated = { inkCanvasRef = it }
                        )
                    } else {
                        ActionsPaneContent(
                            selectedTab = selectedActionTab,
                            onTabSelected = { selectedActionTab = it },
                            note = currentNote,
                            todos = todos,
                            events = events,
                            onToggleTodo = { viewModel.toggleTodo(it) }
                        )
                    }
                }

                // Bottom Bar for S26 Ultra
                NavigationBar(
                    containerColor = BgSurface,
                    contentColor = TextPrimary
                ) {
                    NavigationBarItem(
                        selected = phoneNavTab == 0,
                        onClick = { phoneNavTab = 0 },
                        icon = { Icon(Icons.Default.Brush, contentDescription = "Canvas") },
                        label = { Text("S-Pen") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentCyan,
                            selectedTextColor = AccentCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                    NavigationBarItem(
                        selected = phoneNavTab == 1,
                        onClick = { phoneNavTab = 1 },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Actions") },
                        label = { Text("Actions") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentCyan,
                            selectedTextColor = AccentCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun CanvasPaneContent(
    viewModel: InkActionViewModel,
    currentTool: ToolType,
    currentColor: Int,
    autoPushState: com.inkaction.app.viewmodel.AutoPushUiState,
    onCanvasCreated: (InkCanvasView) -> Unit
) {
    var canvasViewRef by remember { mutableStateOf<InkCanvasView?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Native S-Pen Canvas View
        AndroidView(
            factory = { context ->
                InkCanvasView(context).apply {
                    this.onStrokeStarted = { viewModel.onStrokeStarted() }
                    this.onStrokeFinished = { count ->
                        viewModel.onStrokeFinished(count) { createOcrBitmap() }
                    }
                    canvasViewRef = this
                    onCanvasCreated(this)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.currentTool = currentTool
                view.currentColor = currentColor
            }
        )

        // Floating Pen Toolbar
        FloatingToolbar(
            currentTool = currentTool,
            currentColor = currentColor,
            onSelectTool = { viewModel.setTool(it) },
            onSelectColor = { viewModel.setColor(it) },
            onUndo = { canvasViewRef?.undo() },
            onRedo = { canvasViewRef?.redo() },
            onClear = { canvasViewRef?.clearCanvas() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
        )

        // Bottom Auto-Push & Actionize bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AutoPushCountdownPill(state = autoPushState)

            Button(
                onClick = { viewModel.triggerActionize(canvasViewRef?.createOcrBitmap()) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Actionize", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FloatingToolbar(
    currentTool: ToolType,
    currentColor: Int,
    onSelectTool: (ToolType) -> Unit,
    onSelectColor: (Int) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        AndroidColor.parseColor("#F0F6FC"),
        AndroidColor.parseColor("#58A6FF"),
        AndroidColor.parseColor("#3FB950"),
        AndroidColor.parseColor("#D29922"),
        AndroidColor.parseColor("#F85149"),
        AndroidColor.parseColor("#BC8CFF")
    )

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(BgSurface.copy(alpha = 0.95f))
            .border(1.dp, BorderColor, CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Tool buttons
        IconButton(
            onClick = { onSelectTool(ToolType.PEN) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Pen",
                tint = if (currentTool == ToolType.PEN) AccentCyan else TextMuted
            )
        }

        IconButton(
            onClick = { onSelectTool(ToolType.HIGHLIGHTER) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Brush,
                contentDescription = "Highlighter",
                tint = if (currentTool == ToolType.HIGHLIGHTER) AccentCyan else TextMuted
            )
        }

        IconButton(
            onClick = { onSelectTool(ToolType.ERASER) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Eraser",
                tint = if (currentTool == ToolType.ERASER) AccentCyan else TextMuted
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Color dots
        colors.forEach { colorInt ->
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(colorInt))
                    .border(
                        width = if (currentColor == colorInt) 2.dp else 0.dp,
                        color = if (currentColor == colorInt) Color.White else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onSelectColor(colorInt) }
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        IconButton(onClick = onUndo, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Undo, contentDescription = "Undo", tint = TextMuted)
        }
        IconButton(onClick = onRedo, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Redo, contentDescription = "Redo", tint = TextMuted)
        }
    }
}

@Composable
fun ActionsPaneContent(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    note: com.inkaction.app.ai.NoteDto?,
    todos: List<com.inkaction.app.ai.TodoDto>,
    events: List<com.inkaction.app.ai.EventDto>,
    onToggleTodo: (String) -> Unit
) {
    val tabTitles = listOf("Notes", "Todos", "Calendar")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = BgSurface,
            contentColor = TextPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = AccentBlue
                )
            }
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) TextPrimary else TextMuted
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                0 -> NotesScreen(note = note)
                1 -> TodosScreen(todos = todos, onToggleTodo = onToggleTodo)
                2 -> CalendarScreen(events = events)
            }
        }
    }
}
