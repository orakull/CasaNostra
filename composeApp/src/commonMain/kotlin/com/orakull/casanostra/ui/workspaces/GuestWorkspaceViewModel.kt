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
import kotlinx.coroutines.launch

class GuestWorkspaceViewModel(
    private val repository: WorkspaceRepository
) : ViewModel() {

    var workspace by mutableStateOf<Workspace?>(null)
        private set

    var isLoading by mutableStateOf(true)
        private set

    var error by mutableStateOf<AppError?>(null)
        private set

    fun loadByToken(token: String) {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                workspace = repository.findByShareToken(token)
                if (workspace == null) {
                    error = AppError(
                        userMessage = "Воркспейс не найден",
                        technicalDetail = "findByShareToken() returned null for token: $token"
                    )
                }
            } catch (e: Exception) {
                error = e.toAppError("Не удалось загрузить воркспейс")
            } finally {
                isLoading = false
            }
        }
    }
}
