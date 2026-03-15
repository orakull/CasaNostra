package com.orakull.casanostra.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionSource
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

sealed class AuthState {
    data object Loading : AuthState()
    data object NotAuthenticated : AuthState()
    data object Authenticated : AuthState()
}

class AuthViewModel(
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    var authState by mutableStateOf<AuthState>(AuthState.Loading)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var isProcessing by mutableStateOf(false)
        private set

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        supabaseClient.auth.sessionStatus
            .onEach { status ->
                authState = when (status) {
                    is SessionStatus.Authenticated -> AuthState.Authenticated
                    is SessionStatus.NotAuthenticated -> {
                        if (status.isSignOut) {
                            AuthState.NotAuthenticated
                        } else {
                            AuthState.NotAuthenticated
                        }
                    }
                    is SessionStatus.Initializing -> AuthState.Loading
                    is SessionStatus.RefreshFailure -> AuthState.NotAuthenticated
                }
            }
            .launchIn(viewModelScope)
    }

    fun signUp(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Заполните все поля"
            return
        }
        if (password.length < 6) {
            errorMessage = "Пароль должен быть не менее 6 символов"
            return
        }
        isProcessing = true
        errorMessage = null
        viewModelScope.launch {
            try {
                supabaseClient.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                // After sign up, Supabase auto-confirms if disabled email confirmation
                // The session status flow will update authState automatically
            } catch (e: Exception) {
                errorMessage = parseError(e)
            } finally {
                isProcessing = false
            }
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Заполните все поля"
            return
        }
        isProcessing = true
        errorMessage = null
        viewModelScope.launch {
            try {
                supabaseClient.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
            } catch (e: Exception) {
                errorMessage = parseError(e)
            } finally {
                isProcessing = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                supabaseClient.auth.signOut()
            } catch (e: Exception) {
                errorMessage = parseError(e)
            }
        }
    }

    fun clearError() {
        errorMessage = null
    }

    private fun parseError(e: Exception): String {
        val msg = e.message ?: "Неизвестная ошибка"
        return when {
            "Invalid login credentials" in msg -> "Неверный email или пароль"
            "User already registered" in msg -> "Пользователь уже зарегистрирован"
            "Email not confirmed" in msg -> "Email не подтверждён. Проверьте почту"
            "network" in msg.lowercase() || "connect" in msg.lowercase() -> "Нет подключения к интернету"
            "Unable to validate email address" in msg -> "Некорректный email"
            else -> msg
        }
    }
}
