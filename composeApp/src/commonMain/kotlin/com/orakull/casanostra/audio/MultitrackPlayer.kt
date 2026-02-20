package com.orakull.casanostra.audio

/**
 * Multitrack audio player abstraction.
 * Each platform provides its own implementation.
 */
expect class MultitrackPlayer() {

    /**
     * Load tracks from raw byte arrays (WAV format).
     * Must be called before play().
     */
    fun loadTracks(tracks: List<TrackInfo>)

    /**
     * Start playback of all tracks simultaneously.
     */
    fun play()

    /**
     * Pause playback (can be resumed with play()).
     */
    fun pause()

    /**
     * Stop playback and reset position to 0.
     */
    fun stop()

    /**
     * Seek all tracks to the given position in milliseconds.
     */
    fun seekTo(positionMs: Long)

    /**
     * Set the volume for a specific track.
     * @param trackIndex index of the track
     * @param volume value from 0.0 (silent) to 1.0 (full volume)
     */
    fun setTrackVolume(trackIndex: Int, volume: Float)

    /**
     * Set mute state for a specific track.
     */
    fun setTrackMute(trackIndex: Int, muted: Boolean)

    /**
     * Get current playback position in milliseconds.
     */
    fun getCurrentPositionMs(): Long

    /**
     * Get total duration in milliseconds.
     */
    fun getDurationMs(): Long

    /**
     * Whether playback is currently active.
     */
    fun isPlaying(): Boolean

    /**
     * Release all resources. Player should not be used after this.
     */
    fun release()
}
