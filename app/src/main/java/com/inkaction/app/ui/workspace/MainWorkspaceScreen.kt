package com.inkaction.app.ui.workspace

import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.inkaction.app.R
import com.inkaction.app.data.model.SavedNote
import com.inkaction.app.data.model.SavedTodo
import com.inkaction.app.data.model.NoteFolder
import com.inkaction.app.ui.archive.ArchiveDialog
import com.inkaction.app.ui.calendar.CalendarScreen
import com.inkaction.app.ui.canvas.InkCanvasView
import com.inkaction.app.ui.canvas.ToolType
import com.inkaction.app.ui.notes.NotesScreen
import com.inkaction.app.ui.settings.LanguageDialog
import com.inkaction.app.ui.settings.SettingsDialog
import com.inkaction.app.ui.settings.UpdateDialog
import com.inkaction.app.ui.todos.TodosScreen
import com.inkaction.app.viewmodel.InkActionViewModel

@Composable
fun MainWorkspaceScreen(
    viewModel: InkActionViewModel,
    modifier: Modifier = Modifier
) {
    var inkCanvasRef by remember { mutableStateOf<InkCanvasView?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showArchiveDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val calendarPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.widget.Toast.makeText(context, context.getString(R.string.calendar_perm_granted), android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(context, context.getString(R.string.calendar_perm_denied), android.widget.Toast.LENGTH_LONG).show()
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALENDAR) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            calendarPermissionLauncher.launch(android.Manifest.permission.WRITE_CALENDAR)
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is com.inkaction.app.viewmodel.UiEvent.ShowSnackbar -> {
                    android.widget.Toast.makeText(context, event.message, android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val autoPushState by viewModel.autoPushState.collectAsState()
    val currentTool by viewModel.currentTool.collectAsState()
    val currentColor by viewModel.currentColor.collectAsState()
    val appSettings by viewModel.settings.collectAsState()

    val currentNote by viewModel.currentNote.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()
    val enhancingNotes by viewModel.enhancingNotes.collectAsState()
    val todos by viewModel.allTodos.collectAsState()
    val events by viewModel.events.collectAsState()
    val folders by viewModel.foldersFlow.collectAsState()

    // 0 = Canvas, 1 = Notes, 2 = Todos, 3 = Calendar
    var currentNavTab by remember { mutableIntStateOf(0) }
    var isTabletReferencePanelOpen by remember { mutableStateOf(false) }
    var tabletReferenceTab by remember { mutableIntStateOf(0) }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val success = com.inkaction.app.util.BackupUtil.importFromZip(context, uri, context.filesDir)
            if (success) {
                android.widget.Toast.makeText(context, context.getString(R.string.backup_import_success), android.widget.Toast.LENGTH_LONG).show()
            } else {
                android.widget.Toast.makeText(context, context.getString(R.string.backup_import_error), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val syncState by viewModel.syncState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    if (showSettings) {
        SettingsDialog(
            initialApiKey = appSettings.apiKey,
            initialModel = appSettings.modelName,
            initialReminders = appSettings.remindersEnabled,
            initialLanguage = appSettings.noteLanguage,
            initialThemeMode = appSettings.themeMode,
            initialDefaultEventTime = appSettings.defaultEventTime,
            initialCloudSyncEnabled = appSettings.cloudSyncEnabled,
            initialSyncPassphrase = appSettings.syncPassphrase,
            initialFirebaseProjectId = appSettings.firebaseProjectId,
            initialFirebaseApiKey = appSettings.firebaseApiKey,
            lastSyncTimestamp = appSettings.lastSyncTimestamp,
            syncState = syncState,
            currentUser = currentUser,
            onSignIn = { email, password, onResult -> viewModel.signInWithEmail(email, password, onResult) },
            onRegister = { email, password, onResult -> viewModel.registerWithEmail(email, password, onResult) },
            onSignInWithGoogle = { idToken, onResult -> viewModel.signInWithGoogle(idToken, onResult) },
            onSignOut = { viewModel.signOut() },
            onSaveFirebaseConfig = { projectId, apiKey -> viewModel.setFirebaseConfig(projectId, apiKey) },
            onSyncNow = { pass, enabled -> viewModel.syncNow(pass, enabled) },
            onDismiss = { showSettings = false },
            onSave = { key, model, reminders, language, themeMode, defaultTime, cloudSync, pass ->
                viewModel.saveSettings(key, model, reminders, language, themeMode, defaultTime, cloudSync, pass)
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
                    context.startActivity(android.content.Intent.createChooser(intent, context.getString(R.string.share_backup_title)))
                } else {
                    android.widget.Toast.makeText(context, context.getString(R.string.backup_export_error), android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onImportBackup = {
                importLauncher.launch("application/zip")
            },
            onCheckForUpdates = { showUpdateDialog = true }
        )
    }

    LaunchedEffect(Unit) {
        if (com.inkaction.app.BuildConfig.ENABLE_IN_APP_UPDATER) {
            val update = com.inkaction.app.util.UpdateManager.checkForUpdate(com.inkaction.app.BuildConfig.VERSION_NAME)
            if (update != null) {
                showUpdateDialog = true
            }
        }
    }

    if (showLanguageDialog) {
        LanguageDialog(onDismiss = { showLanguageDialog = false })
    }

    if (showArchiveDialog) {
        ArchiveDialog(
            archivedNotes = allNotes.filter { it.isArchived },
            archivedTodos = todos.filter { it.isArchived },
            onRestoreNote = { viewModel.unarchiveNote(it) },
            onDeleteNotePermanently = { viewModel.deleteNotePermanently(it) },
            onRestoreTodo = { viewModel.unarchiveTodo(it) },
            onDeleteTodoPermanently = { viewModel.deleteTodo(it) },
            onDismiss = { showArchiveDialog = false }
        )
    }

    if (showUpdateDialog) {
        UpdateDialog(onDismiss = { showUpdateDialog = false })
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
        val isTabletLayout = maxWidth >= 700.dp
        val isLandscape = maxWidth > maxHeight

        Row(modifier = Modifier.fillMaxSize()) {
            if (isTabletLayout) {
                // Navigation Rail for Tablet on Left
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(76.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    NavigationRailItem(
                        selected = currentNavTab == 0,
                        onClick = { currentNavTab = 0 },
                        icon = { Icon(Icons.Default.Brush, contentDescription = stringResource(R.string.tab_canvas)) },
                        label = { Text(stringResource(R.string.tab_canvas), fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    NavigationRailItem(
                        selected = currentNavTab == 1,
                        onClick = { currentNavTab = 1 },
                        icon = { Icon(Icons.Default.Description, contentDescription = stringResource(R.string.tab_notes)) },
                        label = { Text(stringResource(R.string.tab_notes), fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    NavigationRailItem(
                        selected = currentNavTab == 2,
                        onClick = { currentNavTab = 2 },
                        icon = { Icon(Icons.Default.Checklist, contentDescription = stringResource(R.string.tab_todos)) },
                        label = { Text(stringResource(R.string.tab_todos), fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    NavigationRailItem(
                        selected = currentNavTab == 3,
                        onClick = { currentNavTab = 3 },
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.tab_calendar)) },
                        label = { Text(stringResource(R.string.tab_calendar), fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            // Right side Column (TopBar + Content + BottomBar if phone)
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
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
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "InkAction",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { 
                                viewModel.saveCurrentNoteManually() 
                                android.widget.Toast.makeText(context, context.getString(R.string.saved), android.widget.Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = stringResource(R.string.save),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        
                        IconButton(
                            onClick = {
                                viewModel.createNewNote()
                                inkCanvasRef?.loadStrokes(emptyList())
                                currentNavTab = 0
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.new_note),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }

                        IconButton(onClick = { viewModel.toggleTemplate() }) {
                            Icon(
                                imageVector = Icons.Default.GridOn,
                                contentDescription = stringResource(R.string.toggle_template),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isTabletLayout && currentNavTab == 0 && isLandscape) {
                            IconButton(onClick = { isTabletReferencePanelOpen = !isTabletReferencePanelOpen }) {
                                Icon(
                                    imageVector = Icons.Default.VerticalSplit,
                                    contentDescription = stringResource(R.string.toggle_reference_panel),
                                    tint = if (isTabletReferencePanelOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        var showOverflowMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.menu),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    text = { 
                                        Column {
                                            Text(stringResource(R.string.auto_push_status), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            Text(
                                                text = if (autoPushState.isProcessing) stringResource(R.string.auto_push_processing) 
                                                       else if (autoPushState.isArmed) stringResource(R.string.auto_push_waiting, autoPushState.remainingSeconds) 
                                                       else stringResource(R.string.auto_push_inactive),
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = { showOverflowMenu = false }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Default.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    },
                                    text = { Text(stringResource(R.string.archive_menu_item)) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showArchiveDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    },
                                    text = { Text(stringResource(R.string.language_menu_item)) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showLanguageDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    },
                                    text = { Text(stringResource(R.string.settings_title)) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showSettings = true
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    },
                                    text = { Text(stringResource(R.string.check_for_updates)) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showUpdateDialog = true
                                    }
                                )
                            }
                        }
                    }
                }

                // Workspace Content Area
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (currentNavTab) {
                        0 -> {
                            if (isTabletLayout && isTabletReferencePanelOpen && isLandscape) {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    Box(modifier = Modifier.weight(0.70f).fillMaxHeight()) {
                                        CanvasPaneContent(
                                            viewModel = viewModel,
                                            currentTool = currentTool,
                                            currentColor = currentColor,
                                            autoPushState = autoPushState,
                                            onCanvasCreated = { inkCanvasRef = it }
                                        )
                                    }
                                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Column(
                                        modifier = Modifier
                                            .weight(0.30f)
                                            .fillMaxHeight()
                                            .background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = stringResource(R.string.reference_panel),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            IconButton(
                                                onClick = { isTabletReferencePanelOpen = false },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = stringResource(R.string.close),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        val refTabs = listOf(
                                            stringResource(R.string.tab_notes),
                                            stringResource(R.string.tab_todos)
                                        )
                                        TabRow(
                                            selectedTabIndex = tabletReferenceTab,
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        ) {
                                            refTabs.forEachIndexed { idx, title ->
                                                Tab(
                                                    selected = tabletReferenceTab == idx,
                                                    onClick = { tabletReferenceTab = idx },
                                                    text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                                                )
                                            }
                                        }
                                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                            if (tabletReferenceTab == 0) {
                                                NotesScreen(
                                                    note = currentNote,
                                                    allNotes = allNotes.filter { !it.isArchived },
                                                    enhancingNotes = enhancingNotes,
                                                    folders = folders,
                                                    onCreateFolder = { name, colorHex -> viewModel.createFolder(name, colorHex) },
                                                    onMoveNote = { noteId, folderId -> viewModel.moveNoteToFolder(noteId, folderId) },
                                                    onResumeDrawing = { noteToResume ->
                                                        viewModel.loadNoteToCanvas(noteToResume)
                                                        viewModel.loadStrokesForNote(noteToResume.id) { strokes ->
                                                            inkCanvasRef?.loadStrokes(strokes)
                                                        }
                                                        isTabletReferencePanelOpen = false
                                                    },
                                                    onDeleteNote = { id -> viewModel.archiveNote(id) },
                                                    onTogglePin = { id -> viewModel.toggleNotePin(id) },
                                                    onEnhanceNote = { id -> viewModel.enhanceNote(id) }
                                                )
                                            } else {
                                                TodosScreen(
                                                    todos = todos.filter { !it.isArchived },
                                                    onToggleTodo = { id, currentStatus -> viewModel.toggleTodo(id, currentStatus) },
                                                    onDeleteTodo = { id -> viewModel.archiveTodo(id) },
                                                    onNavigateToNote = { noteId ->
                                                        val noteToResume = allNotes.find { it.id == noteId }
                                                        if (noteToResume != null) {
                                                            viewModel.loadNoteToCanvas(noteToResume)
                                                            viewModel.loadStrokesForNote(noteToResume.id) { strokes ->
                                                                inkCanvasRef?.loadStrokes(strokes)
                                                            }
                                                            isTabletReferencePanelOpen = false
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                CanvasPaneContent(
                                    viewModel = viewModel,
                                    currentTool = currentTool,
                                    currentColor = currentColor,
                                    autoPushState = autoPushState,
                                    onCanvasCreated = { inkCanvasRef = it }
                                )
                            }
                        }
                        1 -> NotesScreen(
                            note = currentNote,
                            allNotes = allNotes.filter { !it.isArchived },
                            enhancingNotes = enhancingNotes,
                            folders = folders,
                            onCreateFolder = { name, colorHex -> viewModel.createFolder(name, colorHex) },
                            onMoveNote = { noteId, folderId -> viewModel.moveNoteToFolder(noteId, folderId) },
                            onResumeDrawing = { noteToResume ->
                                viewModel.loadNoteToCanvas(noteToResume)
                                viewModel.loadStrokesForNote(noteToResume.id) { strokes ->
                                    inkCanvasRef?.loadStrokes(strokes)
                                }
                                currentNavTab = 0
                            },
                            onDeleteNote = { id -> viewModel.archiveNote(id) },
                            onTogglePin = { id -> viewModel.toggleNotePin(id) },
                            onEnhanceNote = { id -> viewModel.enhanceNote(id) }
                        )
                        2 -> TodosScreen(
                            todos = todos.filter { !it.isArchived },
                            onToggleTodo = { id, currentStatus -> viewModel.toggleTodo(id, currentStatus) },
                            onDeleteTodo = { id -> viewModel.archiveTodo(id) },
                            onNavigateToNote = { noteId ->
                                val noteToResume = allNotes.find { it.id == noteId }
                                if (noteToResume != null) {
                                    viewModel.loadNoteToCanvas(noteToResume)
                                    viewModel.loadStrokesForNote(noteToResume.id) { strokes ->
                                        inkCanvasRef?.loadStrokes(strokes)
                                    }
                                    currentNavTab = 0
                                }
                            }
                        )
                        3 -> CalendarScreen(events = events)
                    }
                }

                // If on Phone, render Bottom NavigationBar
                if (!isTabletLayout) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        NavigationBarItem(
                            selected = currentNavTab == 0,
                            onClick = { currentNavTab = 0 },
                            icon = { Icon(Icons.Default.Brush, contentDescription = stringResource(R.string.tab_canvas)) },
                            label = { Text(stringResource(R.string.tab_canvas)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        NavigationBarItem(
                            selected = currentNavTab == 1,
                            onClick = { currentNavTab = 1 },
                            icon = { Icon(Icons.Default.Description, contentDescription = stringResource(R.string.tab_notes)) },
                            label = { Text(stringResource(R.string.tab_notes)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        NavigationBarItem(
                            selected = currentNavTab == 2,
                            onClick = { currentNavTab = 2 },
                            icon = { Icon(Icons.Default.Checklist, contentDescription = stringResource(R.string.tab_todos)) },
                            label = { Text(stringResource(R.string.tab_todos)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        NavigationBarItem(
                            selected = currentNavTab == 3,
                            onClick = { currentNavTab = 3 },
                            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.tab_calendar)) },
                            label = { Text(stringResource(R.string.tab_calendar)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
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
    val appSettings by viewModel.settings.collectAsState()
    val currentStrokeWidth by viewModel.currentStrokeWidth.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        val systemDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            AndroidView(
                factory = { context ->
                    InkCanvasView(context).apply {
                        this.loadStrokes(viewModel.currentStrokes)
                        this.currentSize = currentStrokeWidth
                        this.onStrokeStarted = { viewModel.onStrokeStarted() }
                        this.onStrokeFinished = { count ->
                            viewModel.currentStrokes.clear()
                            viewModel.currentStrokes.addAll(this.strokes)
                            viewModel.onStrokeFinished(count) { createOcrBitmaps() }
                        }
                        var lastToastTime = 0L
                        this.onFingerTouchBlocked = {
                            val now = System.currentTimeMillis()
                            if (now - lastToastTime > 3500) {
                                lastToastTime = now
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(R.string.stylus_only_hint),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        canvasViewRef = this
                        onCanvasCreated(this)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10000.dp),
                update = { view ->
                    val isDark = appSettings.themeMode == "dark" || (appSettings.themeMode == "system" && systemDarkTheme)
                    view.isDarkMode = isDark
                    view.currentTool = currentTool
                    view.currentColor = currentColor
                    view.currentSize = currentStrokeWidth
                    view.isFingerDrawingEnabled = appSettings.fingerDrawingEnabled
                    view.setTemplate(viewModel.canvasTemplate)
                }
            )
        }

        FloatingToolbar(
            currentTool = currentTool,
            currentColor = currentColor,
            currentStrokeWidth = currentStrokeWidth,
            isFingerDrawingEnabled = appSettings.fingerDrawingEnabled,
            onToggleFingerDrawing = { viewModel.toggleFingerDrawing() },
            onSelectTool = { viewModel.setTool(it) },
            onSelectColor = { colorInt ->
                viewModel.setColor(colorInt)
                if (currentTool != ToolType.PEN && currentTool != ToolType.HIGHLIGHTER) {
                    viewModel.setTool(ToolType.PEN)
                }
            },
            onCycleStrokeWidth = { viewModel.cycleStrokeWidth() },
            onUndo = { canvasViewRef?.undo() },
            onRedo = { canvasViewRef?.redo() },
            onClear = { canvasViewRef?.clearCanvas() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var generateNote by remember { mutableStateOf(true) }
            var generateTodos by remember { mutableStateOf(true) }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { generateNote = !generateNote }
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .padding(end = 12.dp)
            ) {
                Checkbox(
                    checked = generateNote,
                    onCheckedChange = { generateNote = it },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
                Text(
                    stringResource(R.string.checkbox_note),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { generateTodos = !generateTodos }
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .padding(end = 12.dp)
            ) {
                Checkbox(
                    checked = generateTodos,
                    onCheckedChange = { generateTodos = it },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
                Text(
                    stringResource(R.string.checkbox_todos),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = { 
                    if (!autoPushState.isProcessing) {
                        viewModel.triggerActionize(
                            bitmaps = canvasViewRef?.createOcrBitmaps() ?: emptyList(),
                            strokes = canvasViewRef?.strokes?.toList() ?: emptyList(),
                            generateNote = generateNote,
                            generateTodos = generateTodos
                        ) 
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (autoPushState.isProcessing) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary
                ),
                shape = CircleShape
            ) {
                if (autoPushState.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.processing), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.actionize), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FloatingToolbar(
    currentTool: ToolType,
    currentColor: Int,
    currentStrokeWidth: Float,
    isFingerDrawingEnabled: Boolean,
    onToggleFingerDrawing: () -> Unit,
    onSelectTool: (ToolType) -> Unit,
    onSelectColor: (Int) -> Unit,
    onCycleStrokeWidth: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val colors = listOf(
        AndroidColor.parseColor("#F0F6FC"),
        AndroidColor.parseColor("#58A6FF"),
        AndroidColor.parseColor("#3FB950"),
        AndroidColor.parseColor("#D29922"),
        AndroidColor.parseColor("#F85149"),
        AndroidColor.parseColor("#BC8CFF")
    )

    // Dynamic dot diameter corresponding to the 4 stroke widths:
    // 2.5f -> 8.dp (Extra Fine)
    // 4.0f -> 12.dp (Fine / Normal)
    // 6.5f -> 17.dp (Medium)
    // 10.0f -> 23.dp (Thick / Bold)
    val targetDotSize = when {
        currentStrokeWidth <= 3.0f -> 8.dp
        currentStrokeWidth <= 5.0f -> 12.dp
        currentStrokeWidth <= 8.0f -> 17.dp
        else -> 23.dp
    }

    val animatedDotSize by animateDpAsState(
        targetValue = targetDotSize,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "penDotSize"
    )

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = { onSelectTool(ToolType.PEN) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(R.string.tool_pen),
                tint = if (currentTool == ToolType.PEN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(
            onClick = { onSelectTool(ToolType.HIGHLIGHTER) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Brush,
                contentDescription = stringResource(R.string.tool_highlighter),
                tint = if (currentTool == ToolType.HIGHLIGHTER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(
            onClick = { onSelectTool(ToolType.ERASER) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = stringResource(R.string.tool_eraser),
                tint = if (currentTool == ToolType.ERASER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(
            onClick = onToggleFingerDrawing,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = stringResource(if (isFingerDrawingEnabled) R.string.mode_finger_drawing else R.string.mode_stylus_only),
                tint = if (isFingerDrawingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Color bullets with tap-to-cycle stroke width!
        colors.forEach { colorInt ->
            val isSelected = currentColor == colorInt

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (isSelected) {
                            // Already selected -> tap cycles stroke width! The bullet grows and shrinks!
                            onCycleStrokeWidth()
                        } else {
                            // Select this color
                            onSelectColor(colorInt)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Outer ring for selected color
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    )
                }

                // Inner color bullet: dynamically sized when selected, fixed clean dot when unselected
                Box(
                    modifier = Modifier
                        .size(if (isSelected) animatedDotSize else 15.dp)
                        .clip(CircleShape)
                        .background(Color(colorInt))
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        IconButton(onClick = onUndo, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = stringResource(R.string.undo),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onRedo, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Redo,
                contentDescription = stringResource(R.string.redo),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
