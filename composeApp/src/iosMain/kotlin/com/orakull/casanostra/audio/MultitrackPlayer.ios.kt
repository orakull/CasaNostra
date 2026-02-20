package com.orakull.casanostra.audio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.*
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class)
actual class MultitrackPlayer actual constructor() {

    private var engine: AVAudioEngine? = null
    private val playerNodes = mutableListOf<AVAudioPlayerNode>()
    private val audioFiles = mutableListOf<AVAudioFile>()
    private val volumes = mutableListOf<Float>()
    private val muted = mutableListOf<Boolean>()
    private var loaded = false
    private var _isPlaying = false
    private var playStartTime: Double = 0.0  // NSDate timeIntervalSince1970
    private var startOffsetMs: Long = 0L
    private var sampleRate: Double = 44100.0
    private var totalDurationMs: Long = 0L

    actual fun loadTracks(tracks: List<TrackInfo>) {
        release()

        engine = AVAudioEngine()
        val mainMixer = engine!!.mainMixerNode

        tracks.forEach { trackInfo ->
            // Write bytes to a temp file so AVAudioFile can read it
            val tempDir = NSTemporaryDirectory()
            val fileName = "${NSUUID().UUIDString}.wav"
            val filePath = "$tempDir$fileName"
            val fileUrl = NSURL.fileURLWithPath(filePath)

            val nsData = trackInfo.resourceBytes.usePinned { pinned ->
                NSData.dataWithBytes(pinned.addressOf(0), trackInfo.resourceBytes.size.toULong())
            }
            nsData!!.writeToURL(fileUrl, true)

            val audioFile = AVAudioFile(forReading = fileUrl, error = null)!!
            sampleRate = audioFile.processingFormat.sampleRate

            val durationFrames = audioFile.length
            val durationSec = durationFrames.toDouble() / sampleRate
            val durationMsFile = (durationSec * 1000).toLong()
            if (durationMsFile > totalDurationMs) {
                totalDurationMs = durationMsFile
            }

            val playerNode = AVAudioPlayerNode()
            engine!!.attachNode(playerNode)
            engine!!.connect(playerNode, mainMixer, audioFile.processingFormat)

            playerNodes.add(playerNode)
            audioFiles.add(audioFile)
            volumes.add(1.0f)
            muted.add(false)
        }

        engine!!.prepare()
        engine!!.startAndReturnError(null)
        loaded = true
    }

    actual fun play() {
        if (!loaded) return
        if (_isPlaying) return

        // Schedule each audio file from the current offset
        val startFrame = (startOffsetMs * sampleRate / 1000.0).toLong().coerceAtLeast(0)

        playerNodes.forEachIndexed { index, node ->
            val audioFile = audioFiles[index]
            val totalFrames = audioFile.length

            if (startFrame < totalFrames) {
                val remainingFrames = totalFrames - startFrame
                node.scheduleSegment(
                    audioFile,
                    startingFrame = startFrame,
                    frameCount = remainingFrames.toUInt(),
                    atTime = null,
                    completionHandler = null
                )
            }
            node.play()
        }

        playStartTime = NSDate().timeIntervalSince1970
        _isPlaying = true
    }

    actual fun pause() {
        if (!_isPlaying) return
        // Save current position
        startOffsetMs = getCurrentPositionMs()
        playerNodes.forEach { it.stop() }
        _isPlaying = false
    }

    actual fun stop() {
        playerNodes.forEach { it.stop() }
        _isPlaying = false
        startOffsetMs = 0L
    }

    actual fun seekTo(positionMs: Long) {
        val wasPlaying = _isPlaying
        if (_isPlaying) {
            playerNodes.forEach { it.stop() }
            _isPlaying = false
        }
        startOffsetMs = positionMs.coerceAtLeast(0)
        if (wasPlaying) {
            play()
        }
    }

    actual fun setTrackVolume(trackIndex: Int, volume: Float) {
        if (trackIndex !in playerNodes.indices) return
        volumes[trackIndex] = volume
        applyVolume(trackIndex)
    }

    actual fun setTrackMute(trackIndex: Int, muted: Boolean) {
        if (trackIndex !in playerNodes.indices) return
        this.muted[trackIndex] = muted
        applyVolume(trackIndex)
    }

    private fun applyVolume(trackIndex: Int) {
        val effectiveVolume = if (muted[trackIndex]) 0f else volumes[trackIndex]
        playerNodes[trackIndex].volume = effectiveVolume
    }

    actual fun getCurrentPositionMs(): Long {
        if (!_isPlaying) return startOffsetMs
        val elapsed = NSDate().timeIntervalSince1970 - playStartTime
        val currentMs = startOffsetMs + (elapsed * 1000).toLong()
        return currentMs.coerceAtMost(totalDurationMs)
    }

    actual fun getDurationMs(): Long = totalDurationMs

    actual fun isPlaying(): Boolean = _isPlaying

    actual fun release() {
        playerNodes.forEach { node ->
            node.stop()
            engine?.detachNode(node)
        }
        engine?.stop()
        engine = null
        playerNodes.clear()
        audioFiles.clear()
        volumes.clear()
        muted.clear()
        loaded = false
        _isPlaying = false
        startOffsetMs = 0L
        totalDurationMs = 0L
    }
}
