package com.orakull.casanostra.audio

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.datasource.DataSource
import java.util.UUID

private lateinit var appContext: Context

fun initAudioContext(context: Context) {
    appContext = context.applicationContext
}

actual class MultitrackPlayer actual constructor() {

    private val players = mutableListOf<ExoPlayer>()
    private val volumes = mutableListOf<Float>()
    private val muted = mutableListOf<Boolean>()
    private var loaded = false

    @OptIn(UnstableApi::class)
    actual fun loadTracks(tracks: List<TrackInfo>) {
        release()

        tracks.forEach { trackInfo ->
            val player = ExoPlayer.Builder(appContext).build()

            val dataSourceFactory = DataSource.Factory {
                ByteArrayDataSource(trackInfo.resourceBytes)
            }

            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(
                    MediaItem.fromUri(
                        Uri.parse("bytes:///${UUID.randomUUID()}")
                    )
                )

            player.setMediaSource(mediaSource)
            player.prepare()

            players.add(player)
            volumes.add(1.0f)
            muted.add(false)
        }

        loaded = true
    }

    actual fun play() {
        if (!loaded) return
        // Sync all players to the master position before starting
        val masterPos = players.firstOrNull()?.currentPosition ?: 0L
        players.forEach { player ->
            player.seekTo(masterPos)
            player.play()
        }
    }

    actual fun pause() {
        players.forEach { it.pause() }
    }

    actual fun stop() {
        players.forEach { player ->
            player.pause()
            player.seekTo(0)
        }
    }

    actual fun seekTo(positionMs: Long) {
        players.forEach { it.seekTo(positionMs) }
    }

    actual fun setTrackVolume(trackIndex: Int, volume: Float) {
        if (trackIndex !in players.indices) return
        volumes[trackIndex] = volume
        applyVolume(trackIndex)
    }

    actual fun setTrackMute(trackIndex: Int, muted: Boolean) {
        if (trackIndex !in players.indices) return
        this.muted[trackIndex] = muted
        applyVolume(trackIndex)
    }

    private fun applyVolume(trackIndex: Int) {
        val effectiveVolume = if (muted[trackIndex]) 0f else volumes[trackIndex]
        players[trackIndex].volume = effectiveVolume
    }

    actual fun getCurrentPositionMs(): Long {
        return players.firstOrNull()?.currentPosition ?: 0L
    }

    actual fun getDurationMs(): Long {
        return players.firstOrNull()?.duration?.takeIf { it > 0 } ?: 0L
    }

    actual fun isPlaying(): Boolean {
        return players.firstOrNull()?.isPlaying ?: false
    }

    actual fun release() {
        players.forEach { it.release() }
        players.clear()
        volumes.clear()
        muted.clear()
        loaded = false
    }
}
