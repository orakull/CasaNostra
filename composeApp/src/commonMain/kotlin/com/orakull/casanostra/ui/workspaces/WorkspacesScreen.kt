package com.orakull.casanostra.ui.workspaces

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.orakull.casanostra.data.models.Workspace
import com.orakull.casanostra.deeplink.getAppBaseUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspacesScreen(
    viewModel: WorkspacesViewModel,
    onWorkspaceSelected: (Workspace) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var joinError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Мои воркспейсы",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    // Avatar with first letter of email
                    val initial = (viewModel.currentUserEmail ?: "?").first().uppercase()
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Выйти",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(
                    onClick = { showJoinDialog = true },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(Icons.Filled.Link, "Вступить по ссылке")
                }
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Создать")
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
                        val owned = state.workspaces.filter { viewModel.isOwner(it) }
                        val shared = state.workspaces.filter { !viewModel.isOwner(it) }

                        if (state.workspaces.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = Icons.Filled.Folder,
                                    title = "Пока пусто",
                                    subtitle = "Создайте первый воркспейс"
                                )
                            }
                        } else {
                            if (owned.isNotEmpty()) {
                                item {
                                    SectionLabel("Мои")
                                }
                                items(owned) { workspace ->
                                    WorkspaceItem(
                                        workspace = workspace,
                                        isOwner = true,
                                        onShare = { viewModel.generateShareToken(workspace.id, it) },
                                        onClick = { onWorkspaceSelected(workspace) }
                                    )
                                }
                            }

                            if (shared.isNotEmpty()) {
                                item {
                                    SectionLabel("Общие со мной")
                                }
                                items(shared) { workspace ->
                                    WorkspaceItem(
                                        workspace = workspace,
                                        isOwner = false,
                                        onShare = { viewModel.generateShareToken(workspace.id, it) },
                                        onClick = { onWorkspaceSelected(workspace) }
                                    )
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
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
            shape = RoundedCornerShape(24.dp),
            title = { Text("Ошибка") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { joinError = null }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workspace.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (!isOwner) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "Общий",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            if (isOwner) {
                IconButton(onClick = { showShareDialog = true }) {
                    Icon(
                        imageVector = if (shareToken != null) Icons.Filled.Share else Icons.Filled.Lock,
                        contentDescription = "Поделиться",
                        tint = if (shareToken != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    if (showShareDialog) {
        ShareWorkspaceDialog(
            existingToken = shareToken,
            onGenerate = {
                onShare { token ->
                    shareToken = token
                    clipboard.setText(AnnotatedString("${getAppBaseUrl()}/workspace/$token"))
                    showShareDialog = false
                }
            },
            onCopy = {
                shareToken?.let { token ->
                    clipboard.setText(AnnotatedString("${getAppBaseUrl()}/workspace/$token"))
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
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        title = { Text("Новый воркспейс") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name) },
                shape = RoundedCornerShape(16.dp)
            ) { Text("Создать") }
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
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        title = { Text("Вступить по ссылке") },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Ссылка или токен") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = { if (input.isNotBlank()) onJoin(input) },
                shape = RoundedCornerShape(16.dp)
            ) { Text("Вступить") }
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
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
            Button(
                onClick = if (existingToken != null) onCopy else onGenerate,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (existingToken != null) "Скопировать ссылку" else "Создать ссылку")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
