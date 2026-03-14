package com.orakull.casanostra.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.orakull.casanostra.audio.MultitrackPlayer
import com.orakull.casanostra.audio.TrackInfo
import kotlinx.coroutines.*

data class TrackState(
    val name: String,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false
)

class PlayerViewModel : ViewModel() {

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

    fun loadTracks(trackInfos: List<TrackInfo>) {
        val trackNames = listOf("Бас (Вокал)", "Тенор (Фортепиано)", "Тенор (Вокал)")
        player.loadTracks(trackInfos)

        tracks.clear()
        trackInfos.forEachIndexed { index, _ ->
            tracks.add(
                TrackState(
                    name = trackNames.getOrElse(index) { "Дорожка ${index + 1}" }
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
