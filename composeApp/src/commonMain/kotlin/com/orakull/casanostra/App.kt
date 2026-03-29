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
import com.orakull.casanostra.data.models.Workspace
import com.orakull.casanostra.data.repository.ProjectRepository
import com.orakull.casanostra.data.repository.TrackRepository
import com.orakull.casanostra.data.repository.WorkspaceRepository
import com.orakull.casanostra.storage.KEY_PENDING_SHARE_TOKEN
import com.orakull.casanostra.storage.LocalStorage
import com.orakull.casanostra.ui.auth.AuthScreen
import com.orakull.casanostra.ui.auth.AuthState
import com.orakull.casanostra.ui.auth.AuthViewModel
import com.orakull.casanostra.ui.player.PlayerScreen
import com.orakull.casanostra.ui.player.PlayerViewModel
import com.orakull.casanostra.ui.projects.ProjectsScreen
import com.orakull.casanostra.ui.projects.ProjectsViewModel
import com.orakull.casanostra.ui.theme.CasaNostraTheme
import com.orakull.casanostra.ui.workspaces.GuestWorkspaceScreen
import com.orakull.casanostra.ui.workspaces.WorkspacesScreen
import com.orakull.casanostra.ui.workspaces.WorkspacesViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import org.koin.compose.koinInject

@Composable
fun App(initialDeepLinkToken: String? = null) {
    CasaNostraTheme {
        val supabaseClient: SupabaseClient = koinInject()
        val authViewModel: AuthViewModel = viewModel { AuthViewModel(supabaseClient) }
        val authState = authViewModel.authState

        // Tracks whether the user tapped "Sign in" from the guest banner —
        // forces the auth screen even if a token is present
        var guestDismissed by remember { mutableStateOf(false) }

        AnimatedContent(
            targetState = authState,
            transitionSpec = { fadeIn() togetherWith fadeOut() }
        ) { state ->
            when (state) {
                is AuthState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                is AuthState.NotAuthenticated -> {
                    if (initialDeepLinkToken != null && !guestDismissed) {
                        GuestWorkspaceScreen(
                            token = initialDeepLinkToken,
                            onSignIn = { guestDismissed = true }
                        )
                    } else {
                        AuthScreen(viewModel = authViewModel)
                    }
                }

                is AuthState.Authenticated -> {
                    val workspaceRepository = koinInject<WorkspaceRepository>()

                    // Auto-join: when a user signs in/registers and there's a pending share token,
                    // automatically join that workspace (if not already a member and not the owner).
                    LaunchedEffect(initialDeepLinkToken) {
                        val token = initialDeepLinkToken ?: return@LaunchedEffect
                        val userId = supabaseClient.auth.currentUserOrNull()?.id ?: return@LaunchedEffect
                        try {
                            val workspace = workspaceRepository.findByShareToken(token)
                            if (workspace != null && workspace.ownerId != userId) {
                                val alreadyJoined = workspaceRepository.workspaces.value.any { it.id == workspace.id }
                                if (!alreadyJoined) {
                                    workspaceRepository.joinWorkspace(workspace.id, userId)
                                }
                            }
                        } finally {
                            LocalStorage.remove(KEY_PENDING_SHARE_TOKEN)
                        }
                    }

                    var currentScreen by remember { mutableStateOf("workspaces") }
                    var selectedWorkspace by remember { mutableStateOf<Workspace?>(null) }
                    var selectedProject by remember { mutableStateOf<Project?>(null) }
                    val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                    val isReadOnly by remember(selectedWorkspace, currentUserId) {
                        mutableStateOf(selectedWorkspace?.ownerId != currentUserId)
                    }

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            if (targetState == "player" || (targetState == "projects" && initialState == "workspaces")) {
                                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                            } else {
                                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                            }
                        }
                    ) { screen ->
                        when (screen) {
                            "workspaces" -> {
                                val userId = supabaseClient.auth.currentUserOrNull()?.id ?: "guest"
                                val projectRepository = koinInject<ProjectRepository>()
                                val workspacesViewModel: WorkspacesViewModel = viewModel(key = userId) {
                                    WorkspacesViewModel(workspaceRepository, supabaseClient)
                                }
                                WorkspacesScreen(
                                    viewModel = workspacesViewModel,
                                    onWorkspaceSelected = { workspace ->
                                        selectedWorkspace = workspace
                                        projectRepository.clearProjects()
                                        currentScreen = "projects"
                                    },
                                    onLogout = {
                                        workspaceRepository.clearWorkspaces()
                                        authViewModel.signOut()
                                    }
                                )
                            }

                            "projects" -> {
                                val userId = supabaseClient.auth.currentUserOrNull()?.id ?: "guest"
                                val workspaceId = selectedWorkspace?.id
                                val repository = koinInject<ProjectRepository>()
                                val projectsViewModel: ProjectsViewModel = viewModel(
                                    key = "$userId-$workspaceId"
                                ) {
                                    ProjectsViewModel(repository, supabaseClient, workspaceId)
                                }
                                ProjectsScreen(
                                    viewModel = projectsViewModel,
                                    onProjectSelected = { project ->
                                        selectedProject = project
                                        currentScreen = "player"
                                    },
                                    onLogout = {
                                        repository.clearProjects()
                                        currentScreen = "workspaces"
                                    },
                                    workspaceName = selectedWorkspace?.name,
                                    isReadOnly = isReadOnly
                                )
                            }

                            "player" -> {
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
                                    isReadOnly = isReadOnly,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
