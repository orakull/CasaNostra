package com.orakull.casanostra.ui.workspaces

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.orakull.casanostra.data.models.Project
import com.orakull.casanostra.data.repository.ProjectRepository
import com.orakull.casanostra.data.repository.TrackRepository
import com.orakull.casanostra.data.repository.WorkspaceRepository
import com.orakull.casanostra.ui.player.PlayerScreen
import com.orakull.casanostra.ui.player.PlayerViewModel
import com.orakull.casanostra.ui.common.ErrorAlertDialog
import com.orakull.casanostra.ui.projects.ProjectsScreen
import com.orakull.casanostra.ui.projects.ProjectsViewModel
import io.github.jan.supabase.SupabaseClient
import org.koin.compose.koinInject

/**
 * Экран для гостевого доступа по share-ссылке.
 * Загружает воркспейс по токену без авторизации и показывает его проекты
 * в режиме "только просмотр".
 *
 * После регистрации/входа вызывает [onSignIn], который переводит на AuthScreen.
 * В App.kt при переходе в Authenticated-состояние auto-join выполняется автоматически.
 */
@Composable
fun GuestWorkspaceScreen(
    token: String,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val workspaceRepository = koinInject<WorkspaceRepository>()
    val projectRepository = koinInject<ProjectRepository>()
    val trackRepository = koinInject<TrackRepository>()
    val supabaseClient = koinInject<SupabaseClient>()

    val viewModel: GuestWorkspaceViewModel = viewModel(key = token) {
        GuestWorkspaceViewModel(workspaceRepository)
    }

    LaunchedEffect(token) {
        viewModel.loadByToken(token)
    }

    var currentScreen by remember { mutableStateOf("projects") }
    var selectedProject by remember { mutableStateOf<Project?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        // Banner — приглашение зарегистрироваться
        GuestSignInBanner(onSignIn = onSignIn)

        Box(modifier = Modifier.weight(1f)) {
            when {
                viewModel.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                viewModel.error != null -> {
                    // Заглушка (пустой экран) + ErrorAlertDialog поверх
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Воркспейс недоступен",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ErrorAlertDialog(
                        error = viewModel.error!!,
                        onDismiss = onSignIn,
                        onRetry = { viewModel.loadByToken(token) },
                        title = "Не удалось открыть воркспейс"
                    )
                }

                viewModel.workspace != null -> {
                    val workspace = viewModel.workspace!!

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            if (targetState == "player") {
                                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                            } else {
                                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                            }
                        }
                    ) { screen ->
                        when (screen) {
                            "projects" -> {
                                val projectsViewModel: ProjectsViewModel = viewModel(key = "guest-${workspace.id}") {
                                    ProjectsViewModel(projectRepository, supabaseClient, workspace.id)
                                }
                                ProjectsScreen(
                                    viewModel = projectsViewModel,
                                    onProjectSelected = { project ->
                                        selectedProject = project
                                        currentScreen = "player"
                                    },
                                    onLogout = { /* гость не может выйти, кнопка скрыта через isReadOnly */ },
                                    workspaceName = workspace.name,
                                    isReadOnly = true,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            "player" -> {
                                val playerViewModel: PlayerViewModel = viewModel(key = "guest-player") {
                                    PlayerViewModel(projectRepository, trackRepository)
                                }
                                LaunchedEffect(selectedProject) {
                                    selectedProject?.let { playerViewModel.setProject(it, "") }
                                }
                                PlayerScreen(
                                    viewModel = playerViewModel,
                                    onBack = { currentScreen = "projects" },
                                    isReadOnly = true,
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

@Composable
private fun GuestSignInBanner(onSignIn: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Войдите, чтобы сохранить этот воркспейс",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            TextButton(onClick = onSignIn) {
                Text(
                    text = "Войти",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

