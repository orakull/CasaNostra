package com.orakull.casanostra.ui.projects

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orakull.casanostra.data.models.Project
import com.orakull.casanostra.ui.common.AppError
import com.orakull.casanostra.ui.common.toAppError
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import com.orakull.casanostra.data.repository.ProjectRepository

sealed class ProjectsState {
    data object Loading : ProjectsState()
    data class Success(val projects: List<Project>) : ProjectsState()
}

class ProjectsViewModel(
    private val repository: ProjectRepository,
    private val supabaseClient: SupabaseClient,
    val workspaceId: String? = null
) : ViewModel() {

    var state by mutableStateOf<ProjectsState>(ProjectsState.Loading)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    /**
     * Ошибка, отображаемая в AlertDialog поверх текущего контента.
     * Контент (список проектов или заглушка) остаётся видимым.
     */
    var overlayError by mutableStateOf<AppError?>(null)
        private set

    val currentUserEmail: String?
        get() = supabaseClient.auth.currentUserOrNull()?.email

    val currentUserId: String?
        get() = supabaseClient.auth.currentUserOrNull()?.id

    init {
        viewModelScope.launch {
            repository.projects.collect { projects ->
                state = ProjectsState.Success(projects)
            }
        }
        loadProjects()
    }

    fun loadProjects() {
        viewModelScope.launch {
            if (repository.projects.value.isEmpty()) {
                state = ProjectsState.Loading
            }
            try {
                if (workspaceId != null) {
                    repository.fetchProjectsByWorkspace(workspaceId)
                } else {
                    val userId = supabaseClient.auth.currentUserOrNull()?.id
                    if (userId == null) {
                        overlayError = AppError(
                            userMessage = "Вы не авторизованы. Войдите в аккаунт.",
                            technicalDetail = "currentUserOrNull() returned null"
                        )
                        state = ProjectsState.Success(emptyList())
                        return@launch
                    }
                    repository.fetchProjects(userId)
                }
                state = ProjectsState.Success(repository.projects.value)
            } catch (e: Exception) {
                overlayError = e.toAppError("Не удалось загрузить проекты")
                // Не сбрасываем state — показываем что есть (Loading или предыдущий Success)
                if (state is ProjectsState.Loading) {
                    state = ProjectsState.Success(emptyList())
                }
            }
        }
    }

    fun refreshProjects() {
        if (state is ProjectsState.Loading) return
        viewModelScope.launch {
            isRefreshing = true
            try {
                if (workspaceId != null) {
                    repository.fetchProjectsByWorkspace(workspaceId)
                } else {
                    val userId = currentUserId
                    if (userId == null) {
                        overlayError = AppError(
                            userMessage = "Вы не авторизованы. Войдите в аккаунт.",
                            technicalDetail = "currentUserOrNull() returned null"
                        )
                        return@launch
                    }
                    repository.fetchProjects(userId)
                }
            } catch (e: Exception) {
                overlayError = e.toAppError("Не удалось обновить проекты")
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
                repository.createProject(name, userId, workspaceId)
            } catch (e: Exception) {
                overlayError = e.toAppError("Не удалось создать проект")
            }
        }
    }

    fun clearOverlayError() { overlayError = null }
}
