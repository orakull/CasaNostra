package com.orakull.casanostra.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope
import com.orakull.casanostra.data.repository.ProjectRepository
import com.orakull.casanostra.data.repository.TrackRepository
import com.orakull.casanostra.data.models.Project
import com.orakull.casanostra.data.models.ProjectTrack
import com.orakull.casanostra.audio.MultitrackPlayer
import com.orakull.casanostra.audio.TrackInfo
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.cancel

enum class UploadStatus { PENDING, UPLOADING, DONE, ERROR }

data class UploadItemState(
    val file: PlatformFile,
    val status: UploadStatus = UploadStatus.PENDING,
    val progress: Float = 0f,
    val errorMessage: String? = null
)

data class TrackState(
    val name: String,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Transparent
)

class PlayerViewModel(
    private val repository: ProjectRepository,
    private val trackRepository: TrackRepository
) : ViewModel() {

    private val _project = MutableStateFlow<Project?>(null)
    val project: StateFlow<Project?> = _project.asStateFlow()

    // Expose tracks from repository
    val projectTracks: StateFlow<List<ProjectTrack>> = trackRepository.tracks

    var isUploading by mutableStateOf(false)
        private set

    var uploadError by mutableStateOf<String?>(null)
        private set

    // Per-file upload state list
    var uploadItems = mutableStateListOf<UploadItemState>()
        private set

    fun setProject(project: com.orakull.casanostra.data.models.Project) {
        _project.value = project
        tracks.clear()
        player.release()
        isLoaded = false
        
        viewModelScope.launch {
            try {
                trackRepository.fetchTracks(project.id)
                val repoTracks = trackRepository.tracks.value
                if (repoTracks.isEmpty()) {
                    isLoaded = true
                } else {
                    loadProjectTracksIntoPlayer(repoTracks)
                }
            } catch (e: Exception) {
                println("SET_PROJECT_ERROR: ${e.message}")
                isLoaded = true // Stop loader even on error
            }
        }
    }

    private fun loadProjectTracksIntoPlayer(projectTracks: List<com.orakull.casanostra.data.models.ProjectTrack>) {
        viewModelScope.launch {
            try {
                isLoaded = false
                val trackInfos = projectTracks.map { pt ->
                    val bytes = trackRepository.downloadTrackBytes(pt.filePath)
                    com.orakull.casanostra.audio.TrackInfo(name = pt.name, resourceBytes = bytes)
                }
                
                player.loadTracks(trackInfos)

                tracks.clear()
                projectTracks.forEachIndexed { index, pt ->
                    tracks.add(
                        TrackState(
                            name = pt.name,
                            color = trackColorPool[index % trackColorPool.size]
                        )
                    )
                }

                durationMs = player.getDurationMs()
                isLoaded = true
            } catch (e: Exception) {
                println("LOAD_TRACKS_ERROR: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun renameProject(newName: String) {
        val project = _project.value ?: return
        if (newName.isBlank() || newName == project.name) return

        viewModelScope.launch {
            try {
                repository.updateProjectName(project.id, newName)
                _project.update { it?.copy(name = newName) }
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }

    fun deleteProject(onSuccess: () -> Unit) {
        val project = _project.value ?: return
        viewModelScope.launch {
            try {
                repository.deleteProject(project.id)
                _project.value = null
                onSuccess()
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }

    fun uploadAudio(file: PlatformFile) {
        uploadMultipleAudio(listOf(file))
    }

    fun uploadMultipleAudio(files: List<PlatformFile>) {
        val project = _project.value ?: return
        if (files.isEmpty()) return

        // Initialize upload items
        uploadItems.clear()
        files.forEach { uploadItems.add(UploadItemState(file = it)) }
        isUploading = true
        uploadError = null

        viewModelScope.launch {
            // Launch all uploads in parallel
            val jobs = files.mapIndexed { index, file ->
                async {
                    uploadSingleFile(project.id, index, file)
                }
            }
            jobs.awaitAll()

            val anyFailed = uploadItems.any { it.status == UploadStatus.ERROR }
            if (!anyFailed) {
                // All uploaded successfully — reload player
                isUploading = false
                val updatedTracks = trackRepository.tracks.value
                loadProjectTracksIntoPlayer(updatedTracks)
            } else {
                // Keep overlay open with error states to allow Retry
                // isUploading stays true so overlay stays visible
            }
        }
    }

    private suspend fun uploadSingleFile(projectId: String, index: Int, file: PlatformFile) {
        uploadItems[index] = uploadItems[index].copy(status = UploadStatus.UPLOADING, progress = 0.1f)
        try {
            val bytes = file.readBytes()
            uploadItems[index] = uploadItems[index].copy(progress = 0.5f)
            trackRepository.uploadTrack(projectId, file.name, bytes)
            uploadItems[index] = uploadItems[index].copy(status = UploadStatus.DONE, progress = 1f)
        } catch (e: Exception) {
            println("UPLOAD_ERROR[${file.name}]: ${e::class.simpleName}: ${e.message}")
            uploadItems[index] = uploadItems[index].copy(
                status = UploadStatus.ERROR,
                progress = 0f,
                errorMessage = e.message ?: "Неизвестная ошибка"
            )
        }
    }

    fun retryFailedUploads() {
        val project = _project.value ?: return
        val failedIndices = uploadItems.indices.filter { uploadItems[it].status == UploadStatus.ERROR }
        if (failedIndices.isEmpty()) return

        viewModelScope.launch {
            val jobs = failedIndices.map { index ->
                async {
                    uploadSingleFile(project.id, index, uploadItems[index].file)
                }
            }
            jobs.awaitAll()

            val anyFailed = uploadItems.any { it.status == UploadStatus.ERROR }
            if (!anyFailed) {
                isUploading = false
                val updatedTracks = trackRepository.tracks.value
                loadProjectTracksIntoPlayer(updatedTracks)
            }
        }
    }

    fun dismissUploadOverlay() {
        isUploading = false
        uploadItems.clear()
    }

    fun deleteTrack(trackId: String, filePath: String) {
        viewModelScope.launch {
            try {
                trackRepository.deleteTrack(trackId, filePath)
                // Reload all tracks to sync with player state
                loadProjectTracksIntoPlayer(trackRepository.tracks.value)
            } catch (e: Exception) {
                println("DELETE_TRACK_ERROR: ${e.message}")
            }
        }
    }

    fun renameTrack(trackId: String, newName: String) {
        viewModelScope.launch {
            try {
                trackRepository.renameTrack(trackId, newName)
                // Sync with local state list
                val index = trackRepository.tracks.value.indexOfFirst { it.id == trackId }
                if (index != -1 && index < tracks.size) {
                    tracks[index] = tracks[index].copy(name = newName)
                }
            } catch (e: Exception) {
                println("RENAME_TRACK_ERROR: ${e.message}")
            }
        }
    }

    val player = MultitrackPlayer()

    var tracks = mutableStateListOf<TrackState>()
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var currentPositionMs by mutableLongStateOf(0L)
        private set

    var durationMs by mutableLongStateOf(0L)
        private set

    var isLoaded by mutableStateOf(false)
        private set

    private var positionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val trackColorPool = listOf(
        androidx.compose.ui.graphics.Color(0xFFEF9A9A), // Soft Red/Pink (Hue ~0)
        androidx.compose.ui.graphics.Color(0xFFCE93D8),  // Soft Purple (Hue ~280)
        androidx.compose.ui.graphics.Color(0xFF81D4FA), // Soft Blue (Hue ~200)
        androidx.compose.ui.graphics.Color(0xFF80DEEA), // Soft Cyan (Hue ~180)
        androidx.compose.ui.graphics.Color(0xFFA5D6A7), // Soft Green (Hue ~120)
        androidx.compose.ui.graphics.Color(0xFFE6EE9C), // Soft Lime (Hue ~65)
        androidx.compose.ui.graphics.Color(0xFFFFF59D), // Soft Yellow (Hue ~50)
        androidx.compose.ui.graphics.Color(0xFFFFCC80), // Soft Orange (Hue ~35)
    )

    fun loadTracks(trackInfos: List<TrackInfo>) {
        player.loadTracks(trackInfos)

        tracks.clear()
        trackInfos.forEachIndexed { index, info ->
            tracks.add(
                TrackState(
                    name = info.name,
                    color = trackColorPool[index % trackColorPool.size]
                )
            )
        }

        // Wait a moment for ExoPlayer to prepare, then get duration
        scope.launch {
            delay(500)
            durationMs = player.getDurationMs()
            isLoaded = true
        }
    }

    fun play() {
        player.play()
        isPlaying = true
        startPositionUpdates()
    }

    fun pause() {
        player.pause()
        isPlaying = false
        stopPositionUpdates()
    }

    fun stop() {
        player.stop()
        isPlaying = false
        currentPositionMs = 0L
        stopPositionUpdates()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        currentPositionMs = positionMs
    }

    fun setVolume(trackIndex: Int, volume: Float) {
        if (trackIndex !in tracks.indices) return
        tracks[trackIndex] = tracks[trackIndex].copy(volume = volume)
        player.setTrackVolume(trackIndex, volume)
    }

    fun toggleMute(trackIndex: Int) {
        if (trackIndex !in tracks.indices) return
        val newMuted = !tracks[trackIndex].isMuted
        tracks[trackIndex] = tracks[trackIndex].copy(isMuted = newMuted)
        updateEffectiveMutes()
    }

    fun toggleSolo(trackIndex: Int) {
        if (trackIndex !in tracks.indices) return
        val newSolo = !tracks[trackIndex].isSolo
        tracks[trackIndex] = tracks[trackIndex].copy(isSolo = newSolo)
        updateEffectiveMutes()
    }

    private fun updateEffectiveMutes() {
        val anySoloed = tracks.any { it.isSolo }
        tracks.forEachIndexed { index, track ->
            val effectiveMute = if (anySoloed) !track.isSolo else track.isMuted
            player.setTrackMute(index, effectiveMute)
        }
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                currentPositionMs = player.getCurrentPositionMs()
                durationMs = player.getDurationMs().takeIf { it > 0 } ?: durationMs

                // Check if playback ended
                if (durationMs > 0 && currentPositionMs >= durationMs) {
                    player.stop()
                    isPlaying = false
                    currentPositionMs = durationMs
                    stopPositionUpdates()
                    break
                }
                delay(100)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionJob?.cancel()
        positionJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPositionUpdates()
        scope.cancel()
        player.release()
    }
}
