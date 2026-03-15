package com.orakull.casanostra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.orakull.casanostra.data.models.Project

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: ProjectsViewModel,
    onProjectSelected: (Project) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, "Создать проект")
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = viewModel.isRefreshing,
            onRefresh = { viewModel.refreshProjects() },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    AestheticProjectsHeader(
                        userEmail = viewModel.currentUserEmail ?: "Пользователь",
                        onLogout = onLogout
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                when (val state = viewModel.state) {
                    is ProjectsState.Loading -> {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is ProjectsState.Error -> {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    "Ошибка: ${state.message}",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    is ProjectsState.Success -> {
                        if (state.projects.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        "У вас пока нет проектов. Создайте первый!",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(state.projects) { project ->
                                ProjectItem(project, onClick = { onProjectSelected(project) })
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var projectName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Новый мультитрек-проект") },
            text = {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Название проекта") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (projectName.isNotBlank()) {
                        viewModel.createProject(projectName)
                        showCreateDialog = false
                    }
                }) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun ProjectItem(project: Project, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.LibraryMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Создан: ${project.createdAt.take(10)}", // Just showing date prefix roughly
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AestheticProjectsHeader(userEmail: String, onLogout: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
            )
    ) {
        // Logout button moved here
        IconButton(
            onClick = onLogout,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Выйти", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LibraryMusic,
                    contentDescription = "Projects",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = userEmail,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ваши проекты",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
