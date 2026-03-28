package com.orakull.casanostra.cache

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSDataReadingMappedIfSafe
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.darwin.UInt8

actual fun createAudioFileCache(): AudioFileCache = IosAudioFileCache()

@OptIn(ExperimentalForeignApi::class)
private class IosAudioFileCache : AudioFileCache {

    private fun cacheDir(): String {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSCachesDirectory,
            NSUserDomainMask,
            true
        )
        val base = paths.first() as String
        val dir = "$base/audio_tracks"
        NSFileManager.defaultManager.createDirectoryAtPath(
            dir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
        return dir
    }

    private fun keyToPath(key: String): String =
        "${cacheDir()}/${key.replace('/', '_').replace(':', '_')}"

    override suspend fun get(key: String): ByteArray? {
        val path = keyToPath(key)
        if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return null

        val url = NSURL.fileURLWithPath(path)
        // NSData(contentsOf:options:error:) is the correct Kotlin/Native binding
        val data = NSData.create(contentsOfURL = url, options = NSDataReadingMappedIfSafe, error = null)
            ?: return null

        return ByteArray(data.length.toInt()).also { result ->
            result.usePinned { pinned ->
                platform.posix.memcpy(pinned.addressOf(0), data.bytes, data.length)
            }
        }
    }

    override suspend fun put(key: String, data: ByteArray) {
        val path = keyToPath(key)
        data.usePinned { pinned ->
            val nsData = NSData.create(
                bytes = pinned.addressOf(0),
                length = data.size.toULong()
            )
            nsData.writeToFile(path, atomically = true)
        }
    }

    override suspend fun remove(key: String) {
        val path = keyToPath(key)
        if (NSFileManager.defaultManager.fileExistsAtPath(path)) {
            NSFileManager.defaultManager.removeItemAtPath(path, error = null)
        }
    }

    override suspend fun keys(): Set<String> {
        val dir = cacheDir()
        @Suppress("UNCHECKED_CAST")
        val files = NSFileManager.defaultManager
            .contentsOfDirectoryAtPath(dir, error = null)
            as? List<String>
            ?: return emptySet()
        // Reverse the sanitisation: '_' → '/' is an approximation, but filePath keys
        // that had '/' will have been stored as '_'. Good enough for eviction purposes.
        return files.map { it.replace('_', '/') }.toSet()
    }

    override suspend fun clear() {
        val dir = cacheDir()
        @Suppress("UNCHECKED_CAST")
        val files = NSFileManager.defaultManager
            .contentsOfDirectoryAtPath(dir, error = null)
            as? List<String>
            ?: return
        files.forEach { name ->
            NSFileManager.defaultManager.removeItemAtPath("$dir/$name", error = null)
        }
    }
}
