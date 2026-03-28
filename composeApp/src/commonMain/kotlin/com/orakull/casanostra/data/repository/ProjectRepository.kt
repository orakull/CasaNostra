package com.orakull.casanostra.data.repository

import com.orakull.casanostra.data.models.Project
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ProjectRepository(
    private val supabaseClient: SupabaseClient,
    private val cache: com.orakull.casanostra.cache.AudioFileCache
) {
    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    suspend fun fetchProjects(userId: String) {
        val fetchedProjects = supabaseClient.from("projects")
            .select {
                filter {
                    eq("owner_id", userId)
                }
                order("created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }.decodeList<Project>()
        
        _projects.value = fetchedProjects
    }

    suspend fun createProject(name: String, userId: String): Project {
        val newProject = Project(name = name, ownerId = userId)
        val createdProject = supabaseClient.from("projects")
            .insert(newProject) {
                select()
            }.decodeSingle<Project>()
            
        _projects.update { current -> listOf(createdProject) + current }
        return createdProject
    }

    suspend fun updateProjectName(projectId: String, newName: String) {
        // Optimistic update
        _projects.update { current ->
            current.map { if (it.id == projectId) it.copy(name = newName) else it }
        }

        try {
            supabaseClient.from("projects")
                .update({
                    set("name", newName)
                }) {
                    filter {
                        eq("id", projectId)
                    }
                }
        } catch (e: Exception) {
            // Revert on failure
            throw e
        }
    }

    suspend fun deleteProject(projectId: String) {
        val projectToRemove = _projects.value.find { it.id == projectId }
        
        // Optimistic update
        _projects.update { current ->
            current.filter { it.id != projectId }
        }

        try {
            // Fetch tracks to delete them from Storage and cache
            val tracks = supabaseClient.from("project_tracks")
                .select {
                    filter {
                        eq("project_id", projectId)
                    }
                }.decodeList<com.orakull.casanostra.data.models.ProjectTrack>()

            val filePaths = tracks.map { it.filePath }

            if (filePaths.isNotEmpty()) {
                val bucket = supabaseClient.storage["tracks"]
                // Supabase kotlin client allows to pass a list (vararg or collection) but to be safe we iterate
                filePaths.forEach { filePath ->
                    try {
                        bucket.delete(filePath)
                    } catch (e: Exception) {
                        println("Warning: Failed to delete $filePath from Storage: ${e.message}")
                    }
                }
            }

            // Finally, delete the project
            supabaseClient.from("projects")
                .delete {
                    filter {
                        eq("id", projectId)
                    }
                }

            // Clear cache immediately
            filePaths.forEach { filePath ->
                cache.remove(filePath)
                println("CACHE_REMOVE after project delete: $filePath")
            }

        } catch (e: Exception) {
            // Revert on failure
            if (projectToRemove != null) {
                _projects.update { current -> listOf(projectToRemove) + current }
            }
            throw e
        }
    }

    fun clearProjects() {
        _projects.value = emptyList()
    }
}
