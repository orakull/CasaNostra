package com.orakull.casanostra.audio

/**
 * Web platform implementation of MultitrackPlayer.
 *
 * Uses the browser Web Audio API:
 *  - One AudioContext shared across all tracks.
 *  - Each track gets a GainNode (for volume/mute) connected to the destination.
 *  - Playback uses wall-clock time tracking (same as iOS implementation).
 */
actual class MultitrackPlayer actual constructor() {

    private var audioContext: AudioContext? = null

    private data class TrackData(
        val buffer: AudioBuffer,
        var gainNode: GainNode,
        var volume: Float = 1f,
        var muted: Boolean = false,
        var sourceNode: AudioBufferSourceNode? = null
    )

    private val trackDataList = mutableListOf<TrackData>()
    private var loaded = false
    private var _isPlaying = false
    private var playStartContextTime: Double = 0.0
    private var startOffsetMs: Long = 0L
    private var totalDurationMs: Long = 0L

    actual fun loadTracks(tracks: List<TrackInfo>) {
        release()

        audioContext = newAudioContext()
        val ctx = audioContext!!
        val total = tracks.size
        var decoded = 0

        tracks.forEach { trackInfo ->
            val uint8 = byteArrayToUint8Array(trackInfo.resourceBytes)

            decodeAudioDataWithCallbacks(
                ctx = ctx,
                data = uint8,
                onSuccess = { buffer: AudioBuffer ->
                    if (buffer.duration * 1000 > totalDurationMs) {
                        totalDurationMs = (buffer.duration * 1000).toLong()
                    }
                    val gainNode = ctx.createGain()
                    gainNode.connect(ctx.destination)

                    trackDataList.add(
                        TrackData(
                            buffer = buffer,
                            gainNode = gainNode
                        )
                    )
                    decoded++
                    if (decoded == total) {
                        loaded = true
                    }
                },
                onError = { err: JsString ->
                    consoleError("Audio decode error:".toJsString(), err)
                    decoded++
                    if (decoded == total) {
                        loaded = true
                    }
                }
            )
        }
    }

    actual fun play() {
        if (!loaded) return
        if (_isPlaying) return
        val ctx = audioContext ?: return

        ctx.resume()

        val offsetSec = startOffsetMs / 1000.0

        trackDataList.forEach { track ->
            val source = ctx.createBufferSource()
            source.buffer = track.buffer
            source.connect(track.gainNode)
            source.start(0.0, offsetSec)
            track.sourceNode = source
        }

        playStartContextTime = ctx.currentTime - offsetSec
        _isPlaying = true
    }

    actual fun pause() {
        if (!_isPlaying) return
        startOffsetMs = getCurrentPositionMs()
        trackDataList.forEach { track ->
            try { track.sourceNode?.stop() } catch (_: Throwable) {}
            track.sourceNode = null
        }
        _isPlaying = false
    }

    actual fun stop() {
        trackDataList.forEach { track ->
            try { track.sourceNode?.stop() } catch (_: Throwable) {}
            track.sourceNode = null
        }
        _isPlaying = false
        startOffsetMs = 0L
    }

    actual fun seekTo(positionMs: Long) {
        val wasPlaying = _isPlaying
        if (_isPlaying) {
            trackDataList.forEach { track ->
                try { track.sourceNode?.stop() } catch (_: Throwable) {}
                track.sourceNode = null
            }
            _isPlaying = false
        }
        startOffsetMs = positionMs.coerceAtLeast(0L)
        if (wasPlaying) play()
    }

    actual fun setTrackVolume(trackIndex: Int, volume: Float) {
        if (trackIndex !in trackDataList.indices) return
        trackDataList[trackIndex].volume = volume
        applyVolume(trackIndex)
    }

    actual fun setTrackMute(trackIndex: Int, muted: Boolean) {
        if (trackIndex !in trackDataList.indices) return
        trackDataList[trackIndex].muted = muted
        applyVolume(trackIndex)
    }

    private fun applyVolume(trackIndex: Int) {
        val track = trackDataList[trackIndex]
        track.gainNode.gain.value = if (track.muted) 0f else track.volume
    }

    actual fun getCurrentPositionMs(): Long {
        if (!_isPlaying) return startOffsetMs
        val ctx = audioContext ?: return startOffsetMs
        val elapsedSec = ctx.currentTime - playStartContextTime
        return (elapsedSec * 1000).toLong().coerceAtMost(totalDurationMs)
    }

    actual fun getDurationMs(): Long = totalDurationMs

    actual fun isPlaying(): Boolean = _isPlaying

    actual fun release() {
        trackDataList.forEach { track ->
            try { track.sourceNode?.stop() } catch (_: Throwable) {}
        }
        trackDataList.clear()
        loaded = false
        _isPlaying = false
        startOffsetMs = 0L
        totalDurationMs = 0L
        audioContext = null
    }
}
