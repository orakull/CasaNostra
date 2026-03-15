package com.orakull.casanostra.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orakull.casanostra.data.models.Project
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import com.orakull.casanostra.data.repository.ProjectRepository

sealed class ProjectsState {
    data object Loading : ProjectsState()
    data class Success(val projects: List<Project>) : ProjectsState()
    data class Error(val message: String) : ProjectsState()
}

class ProjectsViewModel(
    private val repository: ProjectRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    var state by mutableStateOf<ProjectsState>(ProjectsState.Loading)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    val currentUserEmail: String?
        get() = supabaseClient.auth.currentUserOrNull()?.email

    val currentUserId: String?
        get() = supabaseClient.auth.currentUserOrNull()?.id

    init {
        viewModelScope.launch {
            repository.projects.collect { projects ->
                // Если данные загрузились успешно
                if (state !is ProjectsState.Error || projects.isNotEmpty()) {
                    state = ProjectsState.Success(projects)
                }
            }
        }
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

                repository.fetchProjects(userId)
            } catch (e: Exception) {
                state = ProjectsState.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    fun refreshProjects() {
        if (state is ProjectsState.Loading) return
        viewModelScope.launch {
            isRefreshing = true
            try {
                val userId = currentUserId
                if (userId == null) {
                    state = ProjectsState.Error("Пользователь не авторизован")
                    return@launch
                }

                repository.fetchProjects(userId)
            } catch (e: Exception) {
                state = ProjectsState.Error(e.message ?: "Неизвестная ошибка")
            } finally {
                isRefreshing = false
            }
        }
    }

    fun createProject(name: String) {
        if (name.isBlank()) return

        viewModelScope.launch {
            try {
                val userId = currentUserId ?: return@launch
                repository.createProject(name, userId)
            } catch (e: Exception) {
                state = ProjectsState.Error(e.message ?: "Ошибка создания проекта")
            }
        }
    }
}
