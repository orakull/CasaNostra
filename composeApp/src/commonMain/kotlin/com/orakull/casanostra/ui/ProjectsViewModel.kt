package com.orakull.casanostra.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orakull.casanostra.data.models.Project
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

sealed class ProjectsState {
    data object Loading : ProjectsState()
    data class Success(val projects: List<Project>) : ProjectsState()
    data class Error(val message: String) : ProjectsState()
}

class ProjectsViewModel(
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    var state by mutableStateOf<ProjectsState>(ProjectsState.Loading)
        private set

    val currentUserEmail: String?
        get() = supabaseClient.auth.currentUserOrNull()?.email

    val currentUserId: String?
        get() = supabaseClient.auth.currentUserOrNull()?.id

    init {
        loadProjects()
    }

    fun loadProjects() {
        viewModelScope.launch {
            state = ProjectsState.Loading
            try {
                val userId = supabaseClient.auth.currentUserOrNull()?.id
                if (userId == null) {
                    state = ProjectsState.Error("Пользователь не авторизован")
                    return@launch
                }

                val projects = supabaseClient.postgrest["projects"]
                    .select()
                    .decodeList<Project>()

                state = ProjectsState.Success(projects)
            } catch (e: Exception) {
                state = ProjectsState.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    fun createProject(name: String) {
        if (name.isBlank()) return

        viewModelScope.launch {
            try {
                val userId = supabaseClient.auth.currentUserOrNull()?.id ?: return@launch

                val newProject = Project(
                    name = name,
                    ownerId = userId
                )

                supabaseClient.postgrest["projects"]
                    .insert(newProject)

                // Refresh list using the same suspend function
                val updatedProjects = supabaseClient.postgrest["projects"]
                    .select()
                    .decodeList<Project>()

                state = ProjectsState.Success(updatedProjects)
            } catch (e: Exception) {
                state = ProjectsState.Error(e.message ?: "Ошибка создания проекта")
            }
        }
    }
}
