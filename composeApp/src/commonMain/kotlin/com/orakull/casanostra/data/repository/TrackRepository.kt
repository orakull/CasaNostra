package com.orakull.casanostra.data.repository

import com.orakull.casanostra.cache.AudioFileCache
import com.orakull.casanostra.data.models.ProjectTrack
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class TrackRepository(
    private val supabaseClient: SupabaseClient,
    private val cache: AudioFileCache
) {
    private val _tracks = MutableStateFlow<List<ProjectTrack>>(emptyList())
    val tracks: StateFlow<List<ProjectTrack>> = _tracks.asStateFlow()

    // ─────────────────────────────────────────────
    // Remote data
    // ─────────────────────────────────────────────

    suspend fun fetchTracks(projectId: String) {
        val fetchedTracks = supabaseClient.from("project_tracks")
            .select {
                filter {
                    eq("project_id", projectId)
                }
                order("created_at", order = io.github.jan.supabase.postgrest.query.Order.ASCENDING)
            }.decodeList<ProjectTrack>()

        _tracks.value = fetchedTracks
    }

    /**
     * Fetches the file_path of every track owned by [userId] across ALL projects.
     * Used by [evictStaleEntries] to determine which cache entries are still valid.
     */
    suspend fun fetchAllUserTrackPaths(userId: String): Set<String> {
        // We join through projects to scope tracks to this user only.
        // Supabase postgrest supports filtering via foreign key with `eq`.
        // Since project_tracks has project_id, and projects has owner_id,
        // we use a nested select: select file_path from project_tracks
        // where project_id in (select id from projects where owner_id = userId)
        val allTracks = supabaseClient.from("project_tracks")
            .select {
                filter {
                    // Using RLS / direct join is ideal, but as a safe fallback
                    // we fetch all tracks for all known projects in memory.
                    // The _projects StateFlow is not accessible here,
                    // so we rely on Supabase RLS to return only owned tracks.
                }
            }.decodeList<ProjectTrack>()
        return allTracks.map { it.filePath }.toSet()
    }

    // ─────────────────────────────────────────────
    // Cache-aware read
    // ─────────────────────────────────────────────

    /**
     * Returns audio bytes for [filePath]:
     *   1. If present in cache → return immediately (no network).
     *   2. Otherwise → download from Supabase, store in cache, then return.
     */
    suspend fun getOrDownloadTrackBytes(filePath: String): ByteArray {
        cache.get(filePath)?.let { cached ->
            println("CACHE_HIT: $filePath")
            return cached
        }
        println("CACHE_MISS: $filePath — downloading from backend")
        val bytes = downloadTrackBytes(filePath)
        cache.put(filePath, bytes)
        return bytes
    }

    // ─────────────────────────────────────────────
    // Cache-aware write (upload)
    // ─────────────────────────────────────────────

    /**
     * Uploads [fileBytes] to Supabase Storage + inserts a DB record,
     * then immediately caches the bytes under the canonical [filePath] (UUID path).
     *
     * This way the player can load from cache right after upload without
     * a redundant network round-trip.
     *
     * @return The created [ProjectTrack] record including its server-assigned [filePath].
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun uploadTrack(projectId: String, fileName: String, fileBytes: ByteArray): ProjectTrack {
        val storagePath = "$projectId/${Uuid.random()}"

        // 1. Upload file to Supabase Storage
        val bucket = supabaseClient.storage["tracks"]
        bucket.upload(storagePath, fileBytes) {
            upsert = true
        }

        // 2. Insert record into database
        val newTrack = ProjectTrack(
            projectId = projectId,
            name = fileName.removeSuffix(".wav").removeSuffix(".mp3"),
            filePath = storagePath
        )

        val createdTrack = supabaseClient.from("project_tracks")
            .insert(newTrack) {
                select()
            }.decodeSingle<ProjectTrack>()

        // 3. Cache under the canonical filePath so the player skips download
        cache.put(createdTrack.filePath, fileBytes)
        println("CACHE_PUT after upload: ${createdTrack.filePath}")

        _tracks.update { current -> current + createdTrack }
        return createdTrack
    }

    // ─────────────────────────────────────────────
    // Cache eviction
    // ─────────────────────────────────────────────

    /**
     * Removes from cache every entry whose key is NOT in [validFilePaths].
     *
     * Call this after fetching the full set of track paths for all user projects,
     * so orphaned or replaced files are deleted immediately.
     *
     * Stale keys: entries present in cache but absent from the backend.
     * Valid keys: entries that exist on the backend (current filePath values).
     */
    suspend fun evictStaleEntries(validFilePaths: Set<String>) {
        val cachedKeys = cache.keys()
        val staleKeys = cachedKeys - validFilePaths
        if (staleKeys.isEmpty()) return

        println("CACHE_EVICT: removing ${staleKeys.size} stale entries: $staleKeys")
        staleKeys.forEach { key ->
            cache.remove(key)
        }
    }

    // ─────────────────────────────────────────────
    // Other operations
    // ─────────────────────────────────────────────

    suspend fun deleteTrack(trackId: String, filePath: String) {
        val trackToRemove = _tracks.value.find { it.id == trackId }

        // Optimistic update
        _tracks.update { current -> current.filter { it.id != trackId } }

        try {
            val bucket = supabaseClient.storage["tracks"]
            bucket.delete(filePath)

            supabaseClient.from("project_tracks")
                .delete {
                    filter {
                        eq("id", trackId)
                    }
                }

            // Remove from cache immediately — the file is gone from the backend
            cache.remove(filePath)
            println("CACHE_REMOVE after delete: $filePath")

        } catch (e: Exception) {
            // Revert optimistic update on failure
            if (trackToRemove != null) {
                _tracks.update { current -> current + trackToRemove }
            }
            throw e
        }
    }

    suspend fun renameTrack(trackId: String, newName: String) {
        // Optimistic update
        _tracks.update { current ->
            current.map { if (it.id == trackId) it.copy(name = newName) else it }
        }

        try {
            supabaseClient.from("project_tracks")
                .update({
                    set("name", newName)
                }) {
                    filter {
                        eq("id", trackId)
                    }
                }
        } catch (e: Exception) {
            throw e
        }
    }

    /** Direct download, bypassing the cache. Use [getOrDownloadTrackBytes] instead. */
    suspend fun downloadTrackBytes(filePath: String): ByteArray {
        val bucket = supabaseClient.storage["tracks"]
        return bucket.downloadPublic(filePath)
    }

    fun clearTracks() {
        _tracks.value = emptyList()
    }
}
