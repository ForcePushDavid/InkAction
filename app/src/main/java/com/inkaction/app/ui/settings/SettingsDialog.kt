package com.inkaction.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.inkaction.app.R
import com.inkaction.app.ai.GeminiModelCatalog
import com.inkaction.app.data.sync.SyncState
import com.inkaction.app.data.sync.UserAccountInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    initialApiKey: String,
    initialModel: String,
    initialReminders: Boolean,
    initialLanguage: String,
    initialThemeMode: String,
    initialDefaultEventTime: String,
    initialCloudSyncEnabled: Boolean = false,
    initialSyncPassphrase: String = "",
    initialDebounceDurationMs: Long = 6000L,
    initialFirebaseProjectId: String = "",
    initialFirebaseApiKey: String = "",
    lastSyncTimestamp: Long = 0L,
    syncState: SyncState = SyncState.Idle,
    currentUser: UserAccountInfo? = null,
    onSignIn: ((String, String, (Boolean, String?) -> Unit) -> Unit)? = null,
    onRegister: ((String, String, (Boolean, String?) -> Unit) -> Unit)? = null,
    onSignInWithGoogle: ((String, (Boolean, String?) -> Unit) -> Unit)? = null,
    onSignOut: (() -> Unit)? = null,
    onSaveFirebaseConfig: ((String, String) -> Unit)? = null,
    onSyncNow: ((String, Boolean) -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (
        apiKey: String,
        model: String,
        reminders: Boolean,
        language: String,
        themeMode: String,
        defaultEventTime: String,
        cloudSyncEnabled: Boolean,
        syncPassphrase: String,
        debounceDurationMs: Long
    ) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onCheckForUpdates: () -> Unit = {}
) {
    var apiKey by remember { mutableStateOf(initialApiKey) }
    var model by remember { mutableStateOf(initialModel.ifBlank { "gemini-3.8-flash" }) }
    var remindersEnabled by remember { mutableStateOf(initialReminders) }
    var language by remember { mutableStateOf(initialLanguage) }
    var themeMode by remember { mutableStateOf(initialThemeMode) }
    var defaultEventTime by remember { mutableStateOf(initialDefaultEventTime) }
    var cloudSyncEnabled by remember { mutableStateOf(initialCloudSyncEnabled) }
    var syncPassphrase by remember { mutableStateOf(initialSyncPassphrase) }
    var debounceDurationMs by remember { mutableStateOf(initialDebounceDurationMs) }
    var debounceExpanded by remember { mutableStateOf(false) }

    // Auth Form State
    var authEmail by remember { mutableStateOf("") }
    var authPassword by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var isAuthLoading by remember { mutableStateOf(false) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }
    var showPassphrase by remember { mutableStateOf(false) }

    // Firebase Config Form State
    var showFirebaseConfig by remember { mutableStateOf(false) }
    var firebaseProjectId by remember { mutableStateOf(initialFirebaseProjectId) }
    var firebaseApiKey by remember { mutableStateOf(initialFirebaseApiKey) }

    val context = LocalContext.current
    val googleSignInClient = remember {
        val webClientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            "815669435650-dt040k4j004t6hk9iiis8r4b40sndrp7.apps.googleusercontent.com"
        }
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != android.app.Activity.RESULT_OK) {
            isAuthLoading = false
            authErrorMessage = if (result.resultCode == android.app.Activity.RESULT_CANCELED) {
                "Přihlášení přes Google bylo zrušeno."
            } else {
                "Google přihlášení nebylo dokončeno (kód: ${result.resultCode})."
            }
            return@rememberLauncherForActivityResult
        }
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (!idToken.isNullOrBlank()) {
                onSignInWithGoogle?.invoke(idToken) { success, err ->
                    isAuthLoading = false
                    if (!success) authErrorMessage = err ?: "Chyba přihlášení k Firebase"
                }
            } else {
                isAuthLoading = false
                authErrorMessage = "Nepodařilo se získat Google token."
            }
        } catch (e: ApiException) {
            isAuthLoading = false
            authErrorMessage = when (e.statusCode) {
                10 -> "Chyba 10 (DEVELOPER_ERROR): Zkontrolujte SHA-1 otisk v konzoli Firebase."
                12500 -> "Chyba 12500: Chyba Google Play služeb."
                12501 -> "Přihlášení bylo zrušeno."
                7 -> "Chyba sítě při spojení s Googlem."
                else -> "Chyba Google přihlášení (${e.statusCode}): ${e.localizedMessage}"
            }
        } catch (e: Exception) {
            isAuthLoading = false
            authErrorMessage = e.localizedMessage ?: "Chyba přihlášení přes Google"
        }
    }

    var modelExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }

    val defaultModels = listOf(
        "gemini-3.8-flash",
        "gemini-2.5-flash",
        "gemini-2.5-pro",
        "gemini-2.0-flash"
    )
    var activeModels by remember { mutableStateOf(defaultModels) }
    var isFetchingModels by remember { mutableStateOf(false) }

    LaunchedEffect(apiKey) {
        if (apiKey.isNotBlank() && apiKey.length > 10) {
            isFetchingModels = true
            val fetched = GeminiModelCatalog.fetchAvailableModels(apiKey)
            if (fetched.isNotEmpty()) {
                activeModels = fetched
            }
            isFetchingModels = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.settings_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "InkAction v" + com.inkaction.app.BuildConfig.VERSION_NAME,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    if (com.inkaction.app.BuildConfig.ENABLE_IN_APP_UPDATER) {
                        OutlinedButton(
                            onClick = onCheckForUpdates,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.check_for_updates),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                // -------------------------------------------------------------
                // SECTION 1: CLOUD & PRIVACY (GALAXY S26 ULTRA <-> TAB S9)
                // -------------------------------------------------------------
                SettingsSectionCard(
                    icon = if (cloudSyncEnabled) Icons.Default.CloudSync else Icons.Default.CloudOff,
                    title = stringResource(R.string.section_cloud_sync),
                    iconTint = if (cloudSyncEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.cloud_sync_toggle),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.cloud_sync_toggle_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = cloudSyncEnabled,
                            onCheckedChange = { cloudSyncEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    if (!cloudSyncEnabled) {
                        // Strict Local-Only Privacy Banner
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = stringResource(R.string.cloud_sync_local_only_title),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.cloud_sync_local_only_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        // Cloud Sync is ON:
                        // SUB-SECTION A: USER ACCOUNT (LOGIN / REGISTRATION)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (currentUser != null && currentUser.email.isNotBlank()) {
                                    // Logged in state
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccountCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "Účet synchronizace",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                                Text(
                                                    text = currentUser.email,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = { onSignOut?.invoke() },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(stringResource(R.string.sign_out_button), style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                } else {
                                    // Not logged in -> Google SSO + Email/Password options
                                    OutlinedButton(
                                        onClick = {
                                            isAuthLoading = true
                                            authErrorMessage = null
                                            try {
                                                googleLauncher.launch(googleSignInClient.signInIntent)
                                            } catch (e: Exception) {
                                                isAuthLoading = false
                                                authErrorMessage = "Chyba spuštění přihlášení: ${e.localizedMessage}"
                                            }
                                        },
                                        enabled = !isAuthLoading,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        if (isAuthLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.AccountCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(
                                            text = stringResource(R.string.sign_in_with_google),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    if (authErrorMessage != null) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                        ) {
                                            Text(
                                                text = authErrorMessage!!,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        HorizontalDivider(modifier = Modifier.weight(1f))
                                        Text(
                                            text = stringResource(R.string.or_email_password),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        HorizontalDivider(modifier = Modifier.weight(1f))
                                    }

                                    // Show Tabs: Sign In / Create Account
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = !isRegisterMode,
                                            onClick = {
                                                isRegisterMode = false
                                                authErrorMessage = null
                                            },
                                            label = { Text(stringResource(R.string.sign_in_title)) },
                                            shape = CircleShape
                                        )
                                        FilterChip(
                                            selected = isRegisterMode,
                                            onClick = {
                                                isRegisterMode = true
                                                authErrorMessage = null
                                            },
                                            label = { Text(stringResource(R.string.create_account_title)) },
                                            shape = CircleShape
                                        )
                                    }

                                    OutlinedTextField(
                                        value = authEmail,
                                        onValueChange = { authEmail = it },
                                        label = { Text(stringResource(R.string.email_label)) },
                                        placeholder = { Text("např. david@gmail.com") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = authPassword,
                                        onValueChange = { authPassword = it },
                                        label = { Text(stringResource(R.string.password_label)) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { showPassword = !showPassword }) {
                                                Icon(
                                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        },
                                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    if (authErrorMessage != null) {
                                        Text(
                                            text = authErrorMessage!!,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            if (authEmail.isBlank() || authPassword.isBlank()) {
                                                authErrorMessage = "Vyplňte e-mail a heslo."
                                                return@Button
                                            }
                                            isAuthLoading = true
                                            authErrorMessage = null

                                            if (isRegisterMode) {
                                                onRegister?.invoke(authEmail, authPassword) { success, err ->
                                                    isAuthLoading = false
                                                    if (!success) authErrorMessage = err
                                                }
                                            } else {
                                                onSignIn?.invoke(authEmail, authPassword) { success, err ->
                                                    isAuthLoading = false
                                                    if (!success) authErrorMessage = err
                                                }
                                            }
                                        },
                                        enabled = !isAuthLoading,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isAuthLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(
                                            text = if (isRegisterMode) stringResource(R.string.create_account_button) else stringResource(R.string.sign_in_button),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }

                                    // Expandable custom Firebase config (optional for custom project)
                                    TextButton(
                                        onClick = { showFirebaseConfig = !showFirebaseConfig },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (showFirebaseConfig) "Skrýt Firebase konfiguraci" else "Vlastní Firebase projekt (volitelné)",
                                            fontSize = 11.sp
                                        )
                                    }

                                    AnimatedVisibility(visible = showFirebaseConfig) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = firebaseProjectId,
                                                onValueChange = { firebaseProjectId = it },
                                                label = { Text(stringResource(R.string.firebase_project_id)) },
                                                placeholder = { Text("moje-poznámky-app") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = firebaseApiKey,
                                                onValueChange = { firebaseApiKey = it },
                                                label = { Text(stringResource(R.string.firebase_api_key)) },
                                                placeholder = { Text("AIzaSy...") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )
                                            Button(
                                                onClick = {
                                                    onSaveFirebaseConfig?.invoke(firebaseProjectId.trim(), firebaseApiKey.trim())
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Uložit Firebase projekt", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // SUB-SECTION B: ZERO-KNOWLEDGE E2EE BADGE & PASSPHRASE
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.cloud_sync_e2ee_badge),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.cloud_sync_e2ee_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // PASSPHRASE INPUT FIELD (User explicitly asked to keep this!)
                        OutlinedTextField(
                            value = syncPassphrase,
                            onValueChange = { syncPassphrase = it },
                            label = { Text(stringResource(R.string.cloud_sync_passphrase)) },
                            placeholder = { Text(stringResource(R.string.cloud_sync_passphrase_hint)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { showPassphrase = !showPassphrase }) {
                                    Icon(
                                        imageVector = if (showPassphrase) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            visualTransformation = if (showPassphrase) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // SUB-SECTION C: SYNC TRIGGER & STATUS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val lastSyncStr = if (lastSyncTimestamp > 0) {
                                val sdf = SimpleDateFormat("dd. MM. yyyy, HH:mm", Locale.getDefault())
                                stringResource(R.string.cloud_sync_last_time, sdf.format(Date(lastSyncTimestamp)))
                            } else {
                                stringResource(R.string.cloud_sync_never)
                            }

                            Text(
                                text = lastSyncStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )

                            Button(
                                onClick = { onSyncNow?.invoke(syncPassphrase, cloudSyncEnabled) },
                                shape = RoundedCornerShape(12.dp),
                                enabled = syncState !is SyncState.Syncing,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                if (syncState is SyncState.Syncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.cloud_syncing), style = MaterialTheme.typography.labelMedium)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.cloud_sync_button), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        if (syncState is SyncState.Error) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = (syncState as SyncState.Error).message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        } else if (syncState is SyncState.Success && (syncState as SyncState.Success).message.isNotBlank()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            ) {
                                Text(
                                    text = (syncState as SyncState.Success).message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // SECTION 2: AI & GEMINI MODELS
                // -------------------------------------------------------------
                SettingsSectionCard(
                    icon = Icons.Default.AutoAwesome,
                    title = stringResource(R.string.section_ai),
                    iconTint = MaterialTheme.colorScheme.tertiary
                ) {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("Google Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Text(
                        text = stringResource(R.string.api_key_security_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    ExposedDropdownMenuBox(
                        expanded = modelExpanded,
                        onExpandedChange = { modelExpanded = !modelExpanded }
                    ) {
                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = { Text(stringResource(R.string.active_gemini_model)) },
                            trailingIcon = {
                                if (isFetchingModels) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = modelExpanded,
                            onDismissRequest = { modelExpanded = false }
                        ) {
                            activeModels.forEach { modelId ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = modelId,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    },
                                    onClick = {
                                        model = modelId
                                        modelExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    val languages = listOf("Auto-detect", "English", "Čeština", "Nederlands", "Slovenčina", "Deutsch", "Español", "Français", "Italiano", "Polski")

                    ExposedDropdownMenuBox(
                        expanded = languageExpanded,
                        onExpandedChange = { languageExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = language,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.note_output_language)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
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

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Automatické AI zpracování (Auto-Actionize)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Po dokončení psaní automaticky převede kresbu na AI poznámku a úkoly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val debounceOptions = listOf(
                        4000L to "4 sekundy (Rychlé)",
                        6000L to "6 sekund (Doporučeno)",
                        10000L to "10 sekund",
                        20000L to "20 sekund",
                        0L to "Pouze ručně (Vypnuto)"
                    )

                    val currentDebounceLabel = debounceOptions.find { it.first == debounceDurationMs }?.second ?: "${debounceDurationMs / 1000} s"

                    ExposedDropdownMenuBox(
                        expanded = debounceExpanded,
                        onExpandedChange = { debounceExpanded = !debounceExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentDebounceLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Zpoždění před spuštěním AI") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = debounceExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = debounceExpanded,
                            onDismissRequest = { debounceExpanded = false }
                        ) {
                            debounceOptions.forEach { (duration, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = label,
                                            fontWeight = if (duration == debounceDurationMs) FontWeight.Bold else FontWeight.Normal,
                                            color = if (duration == debounceDurationMs) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        debounceDurationMs = duration
                                        debounceExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // SECTION 3: APPEARANCE & BEHAVIOR
                // -------------------------------------------------------------
                SettingsSectionCard(
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.section_appearance),
                    iconTint = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = stringResource(R.string.app_theme),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf(
                            "system" to stringResource(R.string.theme_system),
                            "dark" to stringResource(R.string.theme_dark),
                            "light" to stringResource(R.string.theme_light)
                        )
                        modes.forEach { (value, label) ->
                            val isSelected = themeMode == value
                            Button(
                                onClick = { themeMode = value },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(label, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.enable_reminders),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                stringResource(R.string.enable_reminders_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = remindersEnabled,
                            onCheckedChange = { remindersEnabled = it }
                        )
                    }

                    OutlinedTextField(
                        value = defaultEventTime,
                        onValueChange = { defaultEventTime = it },
                        label = { Text(stringResource(R.string.default_event_time)) },
                        placeholder = { Text("08:00") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // -------------------------------------------------------------
                // SECTION 4: BACKUP & EXPORT
                // -------------------------------------------------------------
                SettingsSectionCard(
                    icon = Icons.Default.Backup,
                    title = stringResource(R.string.section_backup),
                    iconTint = MaterialTheme.colorScheme.secondary
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onExportBackup,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.export_zip), style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = onImportBackup,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.import_zip), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                apiKey,
                                model,
                                remindersEnabled,
                                language,
                                themeMode,
                                defaultEventTime,
                                cloudSyncEnabled,
                                syncPassphrase,
                                debounceDurationMs
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.save_and_apply))
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionCard(
    icon: ImageVector,
    title: String,
    iconTint: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}
