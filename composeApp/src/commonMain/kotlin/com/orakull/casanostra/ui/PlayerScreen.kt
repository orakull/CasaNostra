package com.orakull.casanostra.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
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
    
    var isEditingName by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Track Editing State
    var selectedTrackId by remember { mutableStateOf<String?>(null) }
    var selectedTrackFilePath by remember { mutableStateOf<String?>(null) }
    var showTrackEditDialog by remember { mutableStateOf(false) }
    var showTrackDeleteDialog by remember { mutableStateOf(false) }

    // FileKit Launcher
    val filePickerLauncher = rememberFilePickerLauncher(
        type = PickerType.File(extensions = listOf("wav", "mp3")),
        title = "Выберите аудиофайл"
    ) { file ->
        file?.let {
            // Because reading file bytes can be blocking/suspend, 
            // the actual reading and uploading happens cleanly in Coroutine.
            viewModel.uploadAudio(it)
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
    ) { paddingValues ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
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
                // Aesthetic Header Region - scrolls away
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

                // Pinned Transport & Progress
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

                // Tracks
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
    } // End Scaffold
} // End PlayerScreen

@Composable
private fun AestheticHeader(
    projectName: String, 
    onBack: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
            )
    ) {
        // Back button in top-left corner
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад к проектам",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Delete button in top-right corner
        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Удалить проект",
                tint = MaterialTheme.colorScheme.error
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = CircleShape
                    )
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = "Music",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = onRenameClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onBackground
                ),
                border = null,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = projectName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Мультитрек-Сессия",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TransportControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onRewind: () -> Unit,
    onStop: () -> Unit,
    isLoaded: Boolean,
    modifier: Modifier = Modifier,
    playButtonSize: androidx.compose.ui.unit.Dp = 56.dp,
    playIconSize: androidx.compose.ui.unit.Dp = 28.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onRewind,
            enabled = isLoaded,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.FastRewind,
                contentDescription = "Rewind",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        FilledIconButton(
            onClick = onPlayPause,
            enabled = isLoaded,
            modifier = Modifier.size(playButtonSize),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(playIconSize)
            )
        }

        IconButton(
            onClick = onStop,
            enabled = isLoaded,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Stop,
                contentDescription = "Stop",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun ProgressBar(
    currentPositionMs: Long,
    durationMs: Long,
    isLoaded: Boolean,
    onSeek: (Long) -> Unit,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val sliderValue = if (durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    if (isLandscape) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = formatTime(currentPositionMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SliderWrapper(sliderValue, durationMs, onSeek, isLoaded, Modifier.weight(1f))
            Text(
                text = formatTime(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Column(modifier = modifier) {
            SliderWrapper(sliderValue, durationMs, onSeek, isLoaded, Modifier.fillMaxWidth())
            Row(
                modifier = Modifier.fillMaxWidth().offset(y = (-4).dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPositionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTime(durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SliderWrapper(
    value: Float,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    isLoaded: Boolean,
    modifier: Modifier = Modifier
) {
    Slider(
        value = value,
        onValueChange = { fraction ->
            onSeek((fraction * durationMs).toLong())
        },
        enabled = isLoaded,
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier.height(24.dp)
    )
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val secStr = if (seconds < 10) "0$seconds" else "$seconds"
    return "$minutes:$secStr"
}
