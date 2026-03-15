package com.orakull.casanostra.data.repository

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

class TrackRepository(private val supabaseClient: SupabaseClient) {
    private val _tracks = MutableStateFlow<List<ProjectTrack>>(emptyList())
    val tracks: StateFlow<List<ProjectTrack>> = _tracks.asStateFlow()

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

    @OptIn(ExperimentalUuidApi::class)
    suspend fun uploadTrack(projectId: String, fileName: String, fileBytes: ByteArray): ProjectTrack {
        // Generate a unique path to prevent overwriting files with the same name
        val storagePath = "$projectId/${Uuid.random()}"

        // 1. Upload file to Supabase Storage
        val bucket = supabaseClient.storage["tracks"]
        bucket.upload(storagePath, fileBytes) {
            upsert = true
        }

        // 2. Insert record into database
        val newTrack = ProjectTrack(
            projectId = projectId,
            name = fileName.removeSuffix(".wav").removeSuffix(".mp3"), // default name
            filePath = storagePath
        )
        
        val createdTrack = supabaseClient.from("project_tracks")
            .insert(newTrack) {
                select()
            }.decodeSingle<ProjectTrack>()

        _tracks.update { current -> current + createdTrack }
        return createdTrack
    }

    suspend fun deleteTrack(trackId: String, filePath: String) {
        val trackToRemove = _tracks.value.find { it.id == trackId }
        
        // Optimistic update
        _tracks.update { current -> current.filter { it.id != trackId } }

        try {
            // 1. Remove file from storage
            val bucket = supabaseClient.storage["tracks"]
            bucket.delete(filePath)

            // 2. Remove record from database
            supabaseClient.from("project_tracks")
                .delete {
                    filter {
                        eq("id", trackId)
                    }
                }
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
            // Assuming rollback requires reloading or keeping the old name, 
            // for simplicity we throw here, a proper production app might revert locally.
            throw e
        }
    }

    fun clearTracks() {
        _tracks.value = emptyList()
    }
}
