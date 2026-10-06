package com.inkaction.app.ui.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkaction.app.BuildConfig
import com.inkaction.app.R
import com.inkaction.app.util.UpdateInfo
import com.inkaction.app.util.UpdateManager
import kotlinx.coroutines.launch
import java.io.File

sealed class UpdateDialogState {
    object Checking : UpdateDialogState()
    object UpToDate : UpdateDialogState()
    data class Available(val info: UpdateInfo) : UpdateDialogState()
    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateDialogState()
    data class ReadyToInstall(val apkFile: File) : UpdateDialogState()
    data class Error(val message: String) : UpdateDialogState()
}

@Composable
fun UpdateDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var state by remember { mutableStateOf<UpdateDialogState>(UpdateDialogState.Checking) }

    LaunchedEffect(Unit) {
        val info = UpdateManager.checkForUpdate(BuildConfig.VERSION_NAME)
        state = if (info != null) {
            UpdateDialogState.Available(info)
        } else {
            UpdateDialogState.UpToDate
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (state !is UpdateDialogState.Downloading) {
                onDismiss()
            }
        },
        title = {
            Text(
                text = when (state) {
                    is UpdateDialogState.Checking -> stringResource(R.string.checking_for_updates)
                    is UpdateDialogState.UpToDate -> stringResource(R.string.check_for_updates)
                    is UpdateDialogState.Available -> stringResource(R.string.update_available_title)
                    is UpdateDialogState.Downloading -> stringResource(R.string.update_available_title)
                    is UpdateDialogState.ReadyToInstall -> stringResource(R.string.update_available_title)
                    is UpdateDialogState.Error -> stringResource(R.string.check_for_updates)
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (val currentState = state) {
                    is UpdateDialogState.Checking -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Text(stringResource(R.string.checking_for_updates), style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    is UpdateDialogState.UpToDate -> {
                        Text(
                            text = stringResource(R.string.no_update_available),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    is UpdateDialogState.Available -> {
                        Text(
                            text = "InkAction v${currentState.info.versionName}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp
                        )
                        if (currentState.info.releaseNotes.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .verticalScroll(rememberScrollState())
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = currentState.info.releaseNotes,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is UpdateDialogState.Downloading -> {
                        val percent = (currentState.progress * 100).toInt()
                        Text(
                            text = stringResource(R.string.downloading_update, percent),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        LinearProgressIndicator(
                            progress = { currentState.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    is UpdateDialogState.ReadyToInstall -> {
                        Text(
                            text = stringResource(R.string.ready_to_install),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    is UpdateDialogState.Error -> {
                        Text(
                            text = currentState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (val currentState = state) {
                is UpdateDialogState.UpToDate -> {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.confirm))
                    }
                }

                is UpdateDialogState.Available -> {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                state = UpdateDialogState.Downloading(currentState.info, 0f)
                                val downloadedFile = UpdateManager.downloadApk(context, currentState.info.downloadUrl) { progress ->
                                    state = UpdateDialogState.Downloading(currentState.info, progress)
                                }
                                if (downloadedFile != null) {
                                    state = UpdateDialogState.ReadyToInstall(downloadedFile)
                                    UpdateManager.installApk(context, downloadedFile)
                                } else {
                                    state = UpdateDialogState.Error(context.getString(R.string.download_failed))
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.update_now))
                    }
                }

                is UpdateDialogState.ReadyToInstall -> {
                    Button(
                        onClick = {
                            UpdateManager.installApk(context, currentState.apkFile)
                        }
                    ) {
                        Text(stringResource(R.string.install))
                    }
                }

                is UpdateDialogState.Error -> {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.close))
                    }
                }

                else -> {}
            }
        },
        dismissButton = {
            if (state !is UpdateDialogState.Downloading && state !is UpdateDialogState.Checking) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}
