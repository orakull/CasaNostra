package com.orakull.casanostra.ui.workspaces

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orakull.casanostra.data.models.Workspace
import com.orakull.casanostra.data.repository.WorkspaceRepository
import com.orakull.casanostra.ui.common.AppError
import com.orakull.casanostra.ui.common.toAppError
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

sealed class WorkspacesState {
    data object Loading : WorkspacesState()
    data class Success(val workspaces: List<Workspace>) : WorkspacesState()
}

class WorkspacesViewModel(
    private val repository: WorkspaceRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    var state by mutableStateOf<WorkspacesState>(WorkspacesState.Loading)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    /**
     * Ошибка, отображаемая в AlertDialog поверх текущего контента.
     * Контент (список воркспейсов или заглушка) остаётся видимым.
     */
    var overlayError by mutableStateOf<AppError?>(null)
        private set

    val currentUserEmail: String?
        get() = supabaseClient.auth.currentUserOrNull()?.email

    private val currentUserId: String?
        get() = supabaseClient.auth.currentUserOrNull()?.id

    init {
        viewModelScope.launch {
            repository.workspaces.collect { workspaces ->
                state = WorkspacesState.Success(workspaces)
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
                    overlayError = AppError(
                        userMessage = "Вы не авторизованы. Войдите в аккаунт.",
                        technicalDetail = "currentUserOrNull() returned null"
                    )
                    state = WorkspacesState.Success(emptyList())
                    return@launch
                }
                repository.fetchWorkspaces(userId)
                state = WorkspacesState.Success(repository.workspaces.value)
            } catch (e: Exception) {
                overlayError = e.toAppError("Не удалось загрузить воркспейсы")
                if (state is WorkspacesState.Loading) {
                    state = WorkspacesState.Success(emptyList())
                }
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
                overlayError = e.toAppError("Не удалось обновить воркспейсы")
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
                overlayError = e.toAppError("Не удалось создать воркспейс")
            }
        }
    }

    fun generateShareToken(workspaceId: String, onToken: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val token = repository.generateShareToken(workspaceId)
                onToken(token)
            } catch (e: Exception) {
                overlayError = e.toAppError("Не удалось создать ссылку для приглашения")
            }
        }
    }

    fun joinByToken(input: String, onSuccess: () -> Unit, onError: (AppError) -> Unit) {
        val token = extractToken(input)
        if (token == null) {
            onError(AppError(
                userMessage = "Неверный формат ссылки или токена",
                technicalDetail = "extractToken() returned null for input: \"$input\""
            ))
            return
        }
        viewModelScope.launch {
            try {
                val workspace = repository.findByShareToken(token)
                if (workspace == null) {
                    onError(AppError(
                        userMessage = "Воркспейс не найден",
                        technicalDetail = "findByShareToken() returned null for token: $token"
                    ))
                    return@launch
                }
                val userId = currentUserId ?: run {
                    onError(AppError(
                        userMessage = "Необходима авторизация",
                        technicalDetail = "currentUserOrNull() returned null"
                    ))
                    return@launch
                }
                if (workspace.ownerId == userId) {
                    onError(AppError(
                        userMessage = "Это ваш собственный воркспейс",
                        technicalDetail = "workspace.ownerId == currentUserId (${userId})"
                    ))
                    return@launch
                }
                val alreadyJoined = repository.workspaces.value.any { it.id == workspace.id }
                if (alreadyJoined) {
                    onError(AppError(
                        userMessage = "Вы уже состоите в этом воркспейсе",
                        technicalDetail = "workspace ${workspace.id} already present in local list"
                    ))
                    return@launch
                }
                repository.joinWorkspace(workspace.id, userId)
                onSuccess()
            } catch (e: Exception) {
                onError(e.toAppError("Не удалось вступить в воркспейс"))
            }
        }
    }

    fun isOwner(workspace: Workspace): Boolean =
        workspace.ownerId == currentUserId

    fun clearOverlayError() { overlayError = null }

    private fun extractToken(input: String): String? {
        val trimmed = input.trim()
        val segment = "/workspace/"
        if (trimmed.contains(segment)) {
            return trimmed.substringAfterLast(segment).trimEnd('/').takeIf { it.isNotBlank() }
        }
        val legacyPrefix = "casanostra://workspace/"
        if (trimmed.startsWith(legacyPrefix)) {
            return trimmed.removePrefix(legacyPrefix).takeIf { it.isNotBlank() }
        }
        return trimmed.takeIf { it.isNotBlank() }
    }
}
