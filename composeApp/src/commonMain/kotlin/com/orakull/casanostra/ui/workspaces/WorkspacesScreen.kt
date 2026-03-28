package com.orakull.casanostra.ui.workspaces

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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.orakull.casanostra.data.models.Workspace

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspacesScreen(
    viewModel: WorkspacesViewModel,
    onWorkspaceSelected: (Workspace) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(viewModel) {
        viewModel.loadWorkspaces()
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var joinError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(
                    onClick = { showJoinDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Filled.Link, "Вступить по ссылке")
                }
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, "Создать воркспейс")
                }
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = viewModel.isRefreshing,
            onRefresh = { viewModel.refreshWorkspaces() },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    WorkspacesHeader(
                        userEmail = viewModel.currentUserEmail ?: "Пользователь",
                        onLogout = onLogout
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                when (val state = viewModel.state) {
                    is WorkspacesState.Loading -> {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is WorkspacesState.Error -> {
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
                    is WorkspacesState.Success -> {
                        if (state.workspaces.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        "Нет воркспейсов. Создайте первый!",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(state.workspaces) { workspace ->
                                WorkspaceItem(
                                    workspace = workspace,
                                    isOwner = viewModel.isOwner(workspace),
                                    onShare = { viewModel.generateShareToken(workspace.id, it) },
                                    onClick = { onWorkspaceSelected(workspace) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateWorkspaceDialog(
            onCreate = { name ->
                viewModel.createWorkspace(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    if (showJoinDialog) {
        JoinWorkspaceDialog(
            onJoin = { input ->
                viewModel.joinByToken(
                    input = input,
                    onSuccess = { showJoinDialog = false },
                    onError = { msg ->
                        joinError = msg
                        showJoinDialog = false
                    }
                )
            },
            onDismiss = { showJoinDialog = false }
        )
    }

    joinError?.let { error ->
        AlertDialog(
            onDismissRequest = { joinError = null },
            title = { Text("Ошибка") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { joinError = null }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun WorkspaceItem(
    workspace: Workspace,
    isOwner: Boolean,
    onShare: ((String) -> Unit) -> Unit,
    onClick: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var showShareDialog by remember { mutableStateOf(false) }
    var shareToken by remember { mutableStateOf(workspace.shareToken) }

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
                Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workspace.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!isOwner) {
                    Text(
                        text = "Shared with you",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isOwner) {
                IconButton(onClick = { showShareDialog = true }) {
                    Icon(
                        imageVector = if (shareToken != null) Icons.Filled.Share else Icons.Filled.Lock,
                        contentDescription = "Поделиться",
                        tint = if (shareToken != null) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showShareDialog) {
        ShareWorkspaceDialog(
            existingToken = shareToken,
            onGenerate = {
                onShare { token ->
                    shareToken = token
                    clipboard.setText(AnnotatedString("casanostra://workspace/$token"))
                    showShareDialog = false
                }
            },
            onCopy = {
                shareToken?.let { token ->
                    clipboard.setText(AnnotatedString("casanostra://workspace/$token"))
                }
                showShareDialog = false
            },
            onDismiss = { showShareDialog = false }
        )
    }
}

@Composable
private fun CreateWorkspaceDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый воркспейс") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onCreate(name) }) { Text("Создать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
private fun JoinWorkspaceDialog(onJoin: (String) -> Unit, onDismiss: () -> Unit) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Вступить по ссылке") },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Ссылка или токен") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            Button(onClick = { if (input.isNotBlank()) onJoin(input) }) { Text("Вступить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
private fun ShareWorkspaceDialog(
    existingToken: String?,
    onGenerate: () -> Unit,
    onCopy: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Поделиться воркспейсом") },
        text = {
            Text(
                if (existingToken != null)
                    "Ссылка уже создана. Скопировать её в буфер обмена?"
                else
                    "Создать ссылку для совместного доступа? По ней любой сможет прослушивать ваши проекты."
            )
        },
        confirmButton = {
            Button(onClick = if (existingToken != null) onCopy else onGenerate) {
                Text(if (existingToken != null) "Скопировать ссылку" else "Создать ссылку")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
private fun WorkspacesHeader(userEmail: String, onLogout: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
            )
    ) {
        IconButton(
            onClick = onLogout,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Выйти",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
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
                text = "Мои воркспейсы",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
