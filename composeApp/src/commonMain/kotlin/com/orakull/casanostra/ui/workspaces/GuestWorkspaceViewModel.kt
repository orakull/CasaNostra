package com.orakull.casanostra.ui.workspaces

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orakull.casanostra.data.models.Workspace
import com.orakull.casanostra.data.repository.WorkspaceRepository
import kotlinx.coroutines.launch

class GuestWorkspaceViewModel(
    private val repository: WorkspaceRepository
) : ViewModel() {

    var workspace by mutableStateOf<Workspace?>(null)
        private set

    var isLoading by mutableStateOf(true)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun loadByToken(token: String) {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                workspace = repository.findByShareToken(token)
                if (workspace == null) {
                    error = "Воркспейс не найден"
                }
            } catch (e: Exception) {
                error = e.message ?: "Ошибка загрузки"
            } finally {
                isLoading = false
            }
        }
    }
}
