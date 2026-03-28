package com.orakull.casanostra.ui.workspaces

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orakull.casanostra.data.models.Workspace
import com.orakull.casanostra.data.repository.WorkspaceRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

sealed class WorkspacesState {
    data object Loading : WorkspacesState()
    data class Success(val workspaces: List<Workspace>) : WorkspacesState()
    data class Error(val message: String) : WorkspacesState()
}

class WorkspacesViewModel(
    private val repository: WorkspaceRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    var state by mutableStateOf<WorkspacesState>(WorkspacesState.Loading)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    val currentUserEmail: String?
        get() = supabaseClient.auth.currentUserOrNull()?.email

    private val currentUserId: String?
        get() = supabaseClient.auth.currentUserOrNull()?.id

    init {
        viewModelScope.launch {
            repository.workspaces.collect { workspaces ->
                if (state !is WorkspacesState.Error || workspaces.isNotEmpty()) {
                    state = WorkspacesState.Success(workspaces)
                }
            }
        }
        loadWorkspaces()
    }

    fun loadWorkspaces() {
        viewModelScope.launch {
            if (repository.workspaces.value.isEmpty()) {
                state = WorkspacesState.Loading
            }
            try {
                val userId = currentUserId
                if (userId == null) {
                    state = WorkspacesState.Error("Пользователь не авторизован")
                    return@launch
                }
                repository.fetchWorkspaces(userId)
                state = WorkspacesState.Success(repository.workspaces.value)
            } catch (e: Exception) {
                state = WorkspacesState.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    fun refreshWorkspaces() {
        if (state is WorkspacesState.Loading) return
        viewModelScope.launch {
            isRefreshing = true
            try {
                val userId = currentUserId ?: return@launch
                repository.fetchWorkspaces(userId)
            } catch (e: Exception) {
                state = WorkspacesState.Error(e.message ?: "Неизвестная ошибка")
            } finally {
                isRefreshing = false
            }
        }
    }

    fun createWorkspace(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val userId = currentUserId ?: return@launch
                repository.createWorkspace(name, userId)
            } catch (e: Exception) {
                state = WorkspacesState.Error(e.message ?: "Ошибка создания воркспейса")
            }
        }
    }

    fun generateShareToken(workspaceId: String, onToken: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val token = repository.generateShareToken(workspaceId)
                onToken(token)
            } catch (e: Exception) {
                state = WorkspacesState.Error(e.message ?: "Ошибка генерации ссылки")
            }
        }
    }

    // Вступить в воркспейс по токену или полной ссылке
    fun joinByToken(input: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val token = extractToken(input)
        if (token == null) {
            onError("Неверный формат ссылки или токена")
            return
        }
        viewModelScope.launch {
            try {
                val workspace = repository.findByShareToken(token)
                if (workspace == null) {
                    onError("Воркспейс не найден")
                    return@launch
                }
                val userId = currentUserId ?: run {
                    onError("Необходима авторизация")
                    return@launch
                }
                // Не вступаем в собственный воркспейс
                if (workspace.ownerId == userId) {
                    onError("Это ваш собственный воркспейс")
                    return@launch
                }
                // Не вступаем повторно
                val alreadyJoined = repository.workspaces.value.any { it.id == workspace.id }
                if (alreadyJoined) {
                    onError("Вы уже в этом воркспейсе")
                    return@launch
                }
                repository.joinWorkspace(workspace.id, userId)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Ошибка при вступлении")
            }
        }
    }

    fun isOwner(workspace: Workspace): Boolean =
        workspace.ownerId == currentUserId

    private fun extractToken(input: String): String? {
        val trimmed = input.trim()
        // Полная ссылка: casanostra://workspace/{token}
        val prefix = "casanostra://workspace/"
        if (trimmed.startsWith(prefix)) {
            return trimmed.removePrefix(prefix).takeIf { it.isNotBlank() }
        }
        // Голый UUID
        return trimmed.takeIf { it.isNotBlank() }
    }
}
