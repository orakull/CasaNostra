package com.orakull.casanostra

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.orakull.casanostra.data.models.Project
import com.orakull.casanostra.data.repository.ProjectRepository
import com.orakull.casanostra.data.repository.TrackRepository
import com.orakull.casanostra.ui.auth.AuthScreen
import com.orakull.casanostra.ui.auth.AuthState
import com.orakull.casanostra.ui.auth.AuthViewModel
import com.orakull.casanostra.ui.player.PlayerScreen
import com.orakull.casanostra.ui.player.PlayerViewModel
import com.orakull.casanostra.ui.projects.ProjectsScreen
import com.orakull.casanostra.ui.projects.ProjectsViewModel
import com.orakull.casanostra.ui.theme.DarkThemeColors
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import org.koin.compose.koinInject

@Composable
fun App() {
    MaterialTheme(colorScheme = DarkThemeColors) {
        val supabaseClient: SupabaseClient = koinInject()
        val authViewModel: AuthViewModel = viewModel { AuthViewModel(supabaseClient) }
        val authState = authViewModel.authState

        AnimatedContent(
            targetState = authState,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            }
        ) { state ->
            when (state) {
                is AuthState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                is AuthState.NotAuthenticated -> {
                    AuthScreen(viewModel = authViewModel)
                }

                is AuthState.Authenticated -> {
                    var currentScreen by remember { mutableStateOf("projects") }
                    var selectedProject by remember { mutableStateOf<Project?>(null) }

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            if (targetState == "player") {
                                slideInHorizontally { width -> width } + fadeIn() togetherWith slideOutHorizontally { width -> -width } + fadeOut()
                            } else {
                                slideInHorizontally { width -> -width } + fadeIn() togetherWith slideOutHorizontally { width -> width } + fadeOut()
                            }
                        }
                    ) { screen ->
                        if (screen == "projects") {
                            val userId = supabaseClient.auth.currentUserOrNull()?.id ?: "guest"
                            val repository = koinInject<ProjectRepository>()
                            val projectsViewModel: ProjectsViewModel = viewModel(key = userId) { ProjectsViewModel(repository, supabaseClient) }
                            ProjectsScreen(
                                viewModel = projectsViewModel,
                                onProjectSelected = { project ->
                                    selectedProject = project
                                    currentScreen = "player"
                                },
                                onLogout = {
                                    repository.clearProjects()
                                    authViewModel.signOut()
                                }
                            )
                        } else if (screen == "player") {
                            val repository = koinInject<ProjectRepository>()
                            val trackRepository = koinInject<TrackRepository>()
                            val playerViewModel: PlayerViewModel = viewModel { PlayerViewModel(repository, trackRepository) }

                            LaunchedEffect(selectedProject) {
                                val userId = supabaseClient.auth.currentUserOrNull()?.id ?: ""
                                selectedProject?.let { playerViewModel.setProject(it, userId) }
                            }

                            PlayerScreen(
                                viewModel = playerViewModel,
                                onBack = { currentScreen = "projects" },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
