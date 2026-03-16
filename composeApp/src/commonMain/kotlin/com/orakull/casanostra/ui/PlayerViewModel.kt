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
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.cancel

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

    fun uploadAudio(file: io.github.vinceglb.filekit.core.PlatformFile) {
        val project = _project.value ?: return
        viewModelScope.launch {
            try {
                isUploading = true
                uploadError = null
                val bytes = file.readBytes()
                trackRepository.uploadTrack(project.id, file.name, bytes)
                
                // After successful upload, reload player tracks to include the new one
                val updatedTracks = trackRepository.tracks.value
                loadProjectTracksIntoPlayer(updatedTracks)
            } catch (e: Exception) {
                println("UPLOAD_ERROR: ${e::class.simpleName}: ${e.message}")
                uploadError = "Ошибка загрузки: ${e.message}"
                e.printStackTrace()
            } finally {
                isUploading = false
            }
        }
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
