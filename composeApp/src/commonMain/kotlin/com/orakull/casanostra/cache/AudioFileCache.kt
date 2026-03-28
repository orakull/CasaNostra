package com.orakull.casanostra.cache

/**
 * Platform-agnostic cache for audio file bytes.
 *
 * Key convention: ProjectTrack.filePath (e.g. "projectId/uuid")
 * — these keys are globally unique across all projects and immutable,
 *   so they serve as perfect cache identifiers.
 *
 * Platforms:
 *   Android  → cacheDir (auto-evicted by OS under memory pressure)
 *   iOS      → NSCachesDirectory (auto-evicted by OS)
 *   wasmJs   → in-memory HashMap (lives for the browser session)
 */
interface AudioFileCache {
    /** Returns cached bytes for [key], or null if not present. */
    suspend fun get(key: String): ByteArray?

    /** Stores [data] under [key], overwriting any existing entry. */
    suspend fun put(key: String, data: ByteArray)

    /** Removes the entry for [key] immediately. No-op if absent. */
    suspend fun remove(key: String)

    /**
     * Returns the set of all keys currently in the cache.
     * Used for cross-project stale-entry detection.
     */
    suspend fun keys(): Set<String>

    /** Removes all entries from the cache. */
    suspend fun clear()
}

/** Platform-specific factory. */
expect fun createAudioFileCache(): AudioFileCache
