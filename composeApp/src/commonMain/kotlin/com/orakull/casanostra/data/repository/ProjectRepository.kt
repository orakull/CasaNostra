package com.orakull.casanostra.data.repository

import com.orakull.casanostra.data.models.Project
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ProjectRepository(private val supabaseClient: SupabaseClient) {
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
            // Revert on failure (we would ideally re-fetch or keep the old state to revert)
            throw e
        }
    }
}
