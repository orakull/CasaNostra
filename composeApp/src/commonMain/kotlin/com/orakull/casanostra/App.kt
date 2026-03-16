package com.orakull.casanostra

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import casanostra.composeapp.generated.resources.Res
import com.orakull.casanostra.audio.TrackInfo
import com.orakull.casanostra.data.models.Project
import com.orakull.casanostra.ui.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.compose.koinInject

val DarkThemeColors = darkColorScheme(
    primary = Color(0xFF00E676),
    onPrimary = Color(0xFF1B1B1F),
    primaryContainer = Color(0xFF1B1B1F),
    onPrimaryContainer = Color(0xFF00E676),
    background = Color(0xFF121215),
    onBackground = Color(0xFFE3E3E8),
    surface = Color(0xFF1B1B1F),
    onSurface = Color(0xFFE3E3E8),
    surfaceVariant = Color(0xFF23232A),
    onSurfaceVariant = Color(0xFFAAAABB),
    error = Color(0xFFCF6679),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@OptIn(ExperimentalResourceApi::class)
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
                            val repository = koinInject<com.orakull.casanostra.data.repository.ProjectRepository>()
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
                            val repository = koinInject<com.orakull.casanostra.data.repository.ProjectRepository>()
                            val trackRepository = koinInject<com.orakull.casanostra.data.repository.TrackRepository>()
                            val playerViewModel: PlayerViewModel = viewModel { PlayerViewModel(repository, trackRepository) }
                            val scope = rememberCoroutineScope()
                            
                            LaunchedEffect(selectedProject) {
                                selectedProject?.let { playerViewModel.setProject(it) }
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