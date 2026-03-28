package com.orakull.casanostra.cache

/**
 * wasmJs (browser) implementation of [AudioFileCache].
 *
 * Lives entirely in memory for the duration of the browser session.
 * This is intentional — the browser has no reliable cross-session
 * file storage API without IndexedDB, and for this use-case the
 * in-memory approach is sufficient: files selected by the user are
 * already in RAM, and remote files are re-downloaded on page reload.
 */
actual fun createAudioFileCache(): AudioFileCache = InMemoryAudioFileCache()

private class InMemoryAudioFileCache : AudioFileCache {

    private val store = mutableMapOf<String, ByteArray>()

    override suspend fun get(key: String): ByteArray? = store[key]

    override suspend fun put(key: String, data: ByteArray) {
        store[key] = data
    }

    override suspend fun remove(key: String) {
        store.remove(key)
    }

    override suspend fun keys(): Set<String> = store.keys.toSet()

    override suspend fun clear() = store.clear()
}
