package com.orakull.casanostra.cache

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android implementation of [AudioFileCache].
 *
 * Stores files in [Context.cacheDir]/audio_tracks/.
 * The OS automatically evicts entries when the device is low on storage.
 *
 * Key sanitisation: '/' and ':' are replaced with '_' to produce a valid filename.
 */

private lateinit var androidCacheContext: Context

/** Called once from [CasaNostraApplication.onCreate]. */
fun initAndroidAudioCache(context: Context) {
    androidCacheContext = context.applicationContext
}

actual fun createAudioFileCache(): AudioFileCache = AndroidAudioFileCache()

private class AndroidAudioFileCache : AudioFileCache {

    private fun cacheDir(): File =
        File(androidCacheContext.cacheDir, "audio_tracks").also { it.mkdirs() }

    private fun keyToFile(key: String): File =
        File(cacheDir(), key.replace('/', '_').replace(':', '_'))

    override suspend fun get(key: String): ByteArray? = withContext(Dispatchers.IO) {
        val file = keyToFile(key)
        if (file.exists()) file.readBytes() else null
    }

    override suspend fun put(key: String, data: ByteArray) = withContext(Dispatchers.IO) {
        keyToFile(key).writeBytes(data)
    }

    override suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        keyToFile(key).delete()
        Unit
    }

    override suspend fun keys(): Set<String> = withContext(Dispatchers.IO) {
        cacheDir().listFiles()
            ?.map { it.name.replace('_', '/') }  // reverse sanitisation (approximate)
            ?.toSet()
            ?: emptySet()
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        cacheDir().listFiles()?.forEach { it.delete() }
        Unit
    }
}
