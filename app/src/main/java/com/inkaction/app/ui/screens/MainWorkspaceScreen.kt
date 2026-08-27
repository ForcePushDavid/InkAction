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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.MaterialTheme
import com.inkaction.app.viewmodel.InkActionViewModel

@Composable
fun MainWorkspaceScreen(
    viewModel: InkActionViewModel,
    modifier: Modifier = Modifier
) {
    var inkCanvasRef by remember { mutableStateOf<InkCanvasView?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val backupImportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val success = com.inkaction.app.util.BackupUtil.importFromZip(context, uri, context.filesDir)
            if (success) {
                android.widget.Toast.makeText(context, "Záloha obnovena. Restartujte aplikaci.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    val calendarPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.widget.Toast.makeText(context, "Přístup ke kalendáři povolen!", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(context, "Bez oprávnění nelze automaticky ukládat do kalendáře.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALENDAR) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            calendarPermissionLauncher.launch(android.Manifest.permission.WRITE_CALENDAR)
        }
    }

    val autoPushState by viewModel.autoPushState.collectAsState()
    val currentTool by viewModel.currentTool.collectAsState()
    val currentColor by viewModel.currentColor.collectAsState()

    val currentNote by viewModel.currentNote.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()
    val enhancingNotes by viewModel.enhancingNotes.collectAsState()
    val todos by viewModel.allTodos.collectAsState()
    val events by viewModel.events.collectAsState()
    val folders by viewModel.foldersFlow.collectAsState()

    var selectedActionTab by remember { mutableIntStateOf(0) }
    var phoneNavTab by remember { mutableIntStateOf(0) } // 0 = Canvas, 1 = Actions

    
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val success = com.inkaction.app.util.BackupUtil.importFromZip(context, uri, context.filesDir)
            if (success) {
                android.widget.Toast.makeText(context, "Záloha importována! Restartujte aplikaci.", android.widget.Toast.LENGTH_LONG).show()
            } else {
                android.widget.Toast.makeText(context, "Chyba při importu.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            initialApiKey = viewModel.apiKey,
            initialModel = viewModel.modelName,
            initialDebounceMs = viewModel.debounceDurationMs,
            initialReminders = viewModel.remindersEnabled,
            initialLanguage = viewModel.noteLanguage,
            initialThemeMode = viewModel.themeMode,
            onDismiss = { showSettings = false },
            onSave = { key, model, debounce, reminders, language, themeMode ->
                viewModel.saveSettings(key, model, debounce, reminders, language, themeMode)
                showSettings = false
            },
            onExportBackup = {
                val uri = com.inkaction.app.util.BackupUtil.exportToZip(context, context.filesDir)
                if (uri != null) {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Sdílet zálohu (ZIP)"))
                } else {
                    android.widget.Toast.makeText(context, "Chyba při vytváření zálohy.", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onImportBackup = {
                importLauncher.launch("application/zip")
            }
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
        val isTabletLayout = maxWidth >= 700.dp // Galaxy Tab S9 vs Galaxy S26 Ultra

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.surface)
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
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "InkAction",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row {
                    IconButton(
                        onClick = { 
                            viewModel.saveCurrentNoteManually() 
                            android.widget.Toast.makeText(context, "Uloženo", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            viewModel.createNewNote()
                            inkCanvasRef?.loadStrokes(emptyList())
                            phoneNavTab = 0
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Note",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    var showMenu by remember { mutableStateOf(false) }
                    
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text("Auto-Push Status", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                        Text(
                                            text = if (autoPushState.isProcessing) "Zpracovávám AI..." 
                                                   else if (autoPushState.isArmed) "Čekám na pauzu (${autoPushState.remainingSeconds}s)" 
                                                   else "Neaktivní (Piš)",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = { showMenu = false }
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.toggleTemplate() }) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Toggle Template",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                            .border(1.dp, MaterialTheme.colorScheme.outline)
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
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        ActionsPaneContent(
                            selectedTab = selectedActionTab,
                            onTabSelected = { selectedActionTab = it },
                            note = currentNote,
                            allNotes = allNotes,
                            enhancingNotes = enhancingNotes,
                            todos = todos,
                            events = events,
                            folders = folders,
                            onCreateFolder = { name, colorHex -> viewModel.createFolder(name, colorHex) },
                            onMoveNote = { noteId, folderId -> viewModel.moveNoteToFolder(noteId, folderId) },
                            onToggleTodo = { id, currentStatus -> viewModel.toggleTodo(id, currentStatus) },
                            onDeleteTodo = { id -> viewModel.deleteTodo(id) },
                            onResumeDrawing = { noteToResume -> 
                                viewModel.loadNoteToCanvas(noteToResume)
                                viewModel.loadStrokesForNote(noteToResume.id) { strokes ->
                                    inkCanvasRef?.loadStrokes(strokes)
                                }
                                phoneNavTab = 0
                            },
                            onDeleteNote = { id -> viewModel.deleteNote(id) },
                            onTogglePin = { id -> viewModel.toggleNotePin(id) },
                            onEnhanceNote = { id -> viewModel.enhanceNote(id) }
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
                            allNotes = allNotes,
                            enhancingNotes = enhancingNotes,
                            todos = todos,
                            events = events,
                            folders = folders,
                            onCreateFolder = { name, colorHex -> viewModel.createFolder(name, colorHex) },
                            onMoveNote = { noteId, folderId -> viewModel.moveNoteToFolder(noteId, folderId) },
                            onToggleTodo = { id, currentStatus -> viewModel.toggleTodo(id, currentStatus) },
                            onDeleteTodo = { id -> viewModel.deleteTodo(id) },
                            onResumeDrawing = { noteToResume -> 
                                viewModel.loadNoteToCanvas(noteToResume)
                                viewModel.loadStrokesForNote(noteToResume.id) { strokes ->
                                    inkCanvasRef?.loadStrokes(strokes)
                                }
                                phoneNavTab = 0
                            },
                            onDeleteNote = { id -> viewModel.deleteNote(id) },
                            onTogglePin = { id -> viewModel.toggleNotePin(id) },
                            onEnhanceNote = { id -> viewModel.enhanceNote(id) }
                        )
                    }
                }

                // Bottom Bar for S26 Ultra
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ) {
                    NavigationBarItem(
                        selected = phoneNavTab == 0,
                        onClick = { phoneNavTab = 0 },
                        icon = { Icon(Icons.Default.Brush, contentDescription = "Canvas") },
                        label = { Text("S-Pen") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.secondary,
                            selectedTextColor = MaterialTheme.colorScheme.secondary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    NavigationBarItem(
                        selected = phoneNavTab == 1,
                        onClick = { phoneNavTab = 1 },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Actions") },
                        label = { Text("Actions") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.secondary,
                            selectedTextColor = MaterialTheme.colorScheme.secondary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
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
    val scrollState = androidx.compose.foundation.rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        val systemDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Native S-Pen Canvas View
            AndroidView(
                factory = { context ->
                    InkCanvasView(context).apply {
                        this.loadStrokes(viewModel.currentStrokes)
                        this.onStrokeStarted = { viewModel.onStrokeStarted() }
                        this.onStrokeFinished = { count ->
                            viewModel.currentStrokes.clear()
                            viewModel.currentStrokes.addAll(this.strokes)
                            viewModel.onStrokeFinished(count) { createOcrBitmaps() }
                        }
                        canvasViewRef = this
                        onCanvasCreated(this)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10000.dp),
                update = { view ->
                    val isDark = viewModel.themeMode == "dark" || (viewModel.themeMode == "system" && systemDarkTheme)
                    view.isDarkMode = isDark
                    view.currentTool = currentTool
                    view.currentColor = currentColor
                    view.setTemplate(viewModel.canvasTemplate)
                }
            )
        }

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

        // Bottom Actionize bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { 
                    if (!autoPushState.isProcessing) {
                        viewModel.triggerActionize(
                            bitmaps = canvasViewRef?.createOcrBitmaps() ?: emptyList(),
                            strokes = canvasViewRef?.strokes?.toList() ?: emptyList()
                        ) 
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (autoPushState.isProcessing) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary
                ),
                shape = CircleShape
            ) {
                if (autoPushState.isProcessing) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Zpracovávám...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else {
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
            .background(androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline, CircleShape)
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
                tint = if (currentTool == ToolType.PEN) androidx.compose.material3.MaterialTheme.colorScheme.secondary else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(
            onClick = { onSelectTool(ToolType.HIGHLIGHTER) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Brush,
                contentDescription = "Highlighter",
                tint = if (currentTool == ToolType.HIGHLIGHTER) androidx.compose.material3.MaterialTheme.colorScheme.secondary else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(
            onClick = { onSelectTool(ToolType.ERASER) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Eraser",
                tint = if (currentTool == ToolType.ERASER) androidx.compose.material3.MaterialTheme.colorScheme.secondary else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
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
            Icon(Icons.Default.Undo, contentDescription = "Undo", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onRedo, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Redo, contentDescription = "Redo", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ActionsPaneContent(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    note: com.inkaction.app.ai.NoteDto?,
    allNotes: List<com.inkaction.app.data.SavedNote>,
    enhancingNotes: Map<Long, Boolean>,
    todos: List<com.inkaction.app.data.SavedTodo>,
    events: List<com.inkaction.app.ai.EventDto>,
    folders: List<com.inkaction.app.data.NoteFolder>,
    onCreateFolder: (String, String) -> Unit,
    onMoveNote: (Long, Long?) -> Unit,
    onToggleTodo: (String, Boolean) -> Unit,
    onDeleteTodo: (String) -> Unit,
    onResumeDrawing: (com.inkaction.app.data.SavedNote) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onTogglePin: (Long) -> Unit,
    onEnhanceNote: (Long) -> Unit
) {
    val tabTitles = listOf("Notes", "Todos", "Calendar")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                )
            }
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    text = { Text(title, fontWeight = FontWeight.Bold) }
                )
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                0 -> NotesScreen(
                    note = note, 
                    allNotes = allNotes, 
                    enhancingNotes = enhancingNotes,
                    folders = folders,
                    onCreateFolder = onCreateFolder,
                    onMoveNote = onMoveNote,
                    onResumeDrawing = onResumeDrawing, 
                    onDeleteNote = onDeleteNote,
                    onTogglePin = onTogglePin,
                    onEnhanceNote = onEnhanceNote
                )
                1 -> TodosScreen(
                    todos = todos, 
                    onToggleTodo = onToggleTodo, 
                    onDeleteTodo = onDeleteTodo,
                    onNavigateToNote = { noteId ->
                        val noteToResume = allNotes.find { it.id == noteId }
                        if (noteToResume != null) {
                            onResumeDrawing(noteToResume)
                        }
                    }
                )
                2 -> CalendarScreen(events = events)
            }
        }
    }
}
