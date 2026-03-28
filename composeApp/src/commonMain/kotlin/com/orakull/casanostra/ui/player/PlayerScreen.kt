package com.orakull.casanostra.ui.player

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.orakull.casanostra.ui.common.LockScreenOrientation
import com.orakull.casanostra.ui.common.ScreenOrientation
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LockScreenOrientation(ScreenOrientation.Unspecified)

    val isPlaying = viewModel.isPlaying
    val currentPositionMs = viewModel.currentPositionMs
    val durationMs = viewModel.durationMs
    val tracks = viewModel.tracks
    val isLoaded = viewModel.isLoaded
    val project by viewModel.project.collectAsState()
    val projectTracks by viewModel.projectTracks.collectAsState()
    val isUploading = viewModel.isUploading

    var isEditingName by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    var selectedTrackId by remember { mutableStateOf<String?>(null) }
    var selectedTrackFilePath by remember { mutableStateOf<String?>(null) }
    var showTrackEditDialog by remember { mutableStateOf(false) }
    var showTrackDeleteDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberFilePickerLauncher(
        type = PickerType.File(extensions = listOf("wav", "mp3")),
        mode = PickerMode.Multiple(),
        title = "Выберите аудиофайлы"
    ) { files ->
        files?.takeIf { it.isNotEmpty() }?.let {
            viewModel.uploadMultipleAudio(it)
        }
    }

    Scaffold(
        floatingActionButton = {
            if (isLoaded) {
                FloatingActionButton(
                    onClick = { filePickerLauncher.launch() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить дорожку")
                }
            }
        },
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        if (!isLoaded) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isLandscape = maxWidth > maxHeight

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                if (!isLandscape) {
                    item {
                        AestheticHeader(
                            projectName = project?.name ?: "Проект",
                            onBack = onBack,
                            onRenameClick = { isEditingName = true },
                            onDeleteClick = { showDeleteConfirmation = true }
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                stickyHeader {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                        shadowElevation = 8.dp
                    ) {
                        if (isLandscape) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TransportControls(
                                    isPlaying = isPlaying,
                                    onPlayPause = { if (isPlaying) viewModel.pause() else viewModel.play() },
                                    onRewind = { viewModel.seekTo(0) },
                                    onStop = { viewModel.stop() },
                                    isLoaded = isLoaded,
                                    playButtonSize = 48.dp,
                                    playIconSize = 24.dp,
                                    modifier = Modifier.padding(end = 24.dp)
                                )
                                ProgressBar(
                                    currentPositionMs = currentPositionMs,
                                    durationMs = durationMs,
                                    isLoaded = isLoaded,
                                    onSeek = { viewModel.seekTo(it) },
                                    isLandscape = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                            ) {
                                ProgressBar(
                                    currentPositionMs = currentPositionMs,
                                    durationMs = durationMs,
                                    isLoaded = isLoaded,
                                    onSeek = { viewModel.seekTo(it) },
                                    isLandscape = false,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TransportControls(
                                    isPlaying = isPlaying,
                                    onPlayPause = { if (isPlaying) viewModel.pause() else viewModel.play() },
                                    onRewind = { viewModel.seekTo(0) },
                                    onStop = { viewModel.stop() },
                                    isLoaded = isLoaded,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                if (tracks.isEmpty() && isLoaded) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.LibraryMusic,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "В проекте пока нет дорожек",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Нажмите + чтобы добавить аудио",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                itemsIndexed(tracks) { index, track ->
                    TrackRow(
                        track = track,
                        index = index,
                        onVolumeChange = { volume -> viewModel.setVolume(index, volume) },
                        onMuteToggle = { viewModel.toggleMute(index) },
                        onSoloToggle = { viewModel.toggleSolo(index) },
                        onTrackClick = {
                            val dbTrack = projectTracks.getOrNull(index)
                            if (dbTrack != null) {
                                selectedTrackId = dbTrack.id
                                selectedTrackFilePath = dbTrack.filePath
                                showTrackEditDialog = true
                            }
                        }
                    )
                }
            }
        }
    }

    if (isEditingName) {
        var newName by remember { mutableStateOf(project?.name ?: "") }
        val focusRequester = remember { FocusRequester() }

        AlertDialog(
            onDismissRequest = { isEditingName = false },
            title = { Text("Переименовать проект") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Название") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.focusRequester(focusRequester)
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.renameProject(newName)
                        isEditingName = false
                    }
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditingName = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Удалить проект?") },
            text = { Text("Проект \"${project?.name ?: ""}\" будет удален безвозвратно. Это действие нельзя отменить.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProject(onSuccess = onBack)
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showTrackEditDialog && selectedTrackId != null) {
        val currentTrackName = projectTracks.find { it.id == selectedTrackId }?.name ?: ""
        var newTrackName by remember { mutableStateOf(currentTrackName) }
        val focusRequester = remember { FocusRequester() }

        AlertDialog(
            onDismissRequest = { showTrackEditDialog = false },
            title = { Text("Опции Трека") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTrackName,
                        onValueChange = { newTrackName = it },
                        label = { Text("Название") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.focusRequester(focusRequester).fillMaxWidth()
                    )
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            showTrackEditDialog = false
                            showTrackDeleteDialog = true
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Удалить трек")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Удалить трек из проекта")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newTrackName.isNotBlank() && newTrackName != currentTrackName) {
                        viewModel.renameTrack(selectedTrackId!!, newTrackName)
                    }
                    showTrackEditDialog = false
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTrackEditDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showTrackDeleteDialog && selectedTrackId != null && selectedTrackFilePath != null) {
        AlertDialog(
            onDismissRequest = { showTrackDeleteDialog = false },
            title = { Text("Удалить трек?") },
            text = { Text("Этот трек и аудиофайл будут удалены безвозвратно.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTrack(selectedTrackId!!, selectedTrackFilePath!!)
                        showTrackDeleteDialog = false
                        selectedTrackId = null
                        selectedTrackFilePath = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTrackDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    } // End Column

    // Upload Progress Overlay
    if (isUploading) {
        UploadProgressOverlay(
            uploadItems = viewModel.uploadItems,
            onRetry = { viewModel.retryFailedUploads() },
            onDismiss = { viewModel.dismissUploadOverlay() }
        )
    }

    } // End Scaffold
} // End PlayerScreen
