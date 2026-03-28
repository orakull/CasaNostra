package com.orakull.casanostra.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun UploadProgressOverlay(
    uploadItems: List<UploadItemState>,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val hasErrors = uploadItems.any { it.status == UploadStatus.ERROR }
    val allDone = uploadItems.isNotEmpty() && uploadItems.all { it.status == UploadStatus.DONE }
    val isStillUploading = uploadItems.any { it.status == UploadStatus.UPLOADING || it.status == UploadStatus.PENDING }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when {
                        allDone -> "Загрузка завершена ✓"
                        hasErrors && !isStillUploading -> "Ошибка загрузки"
                        else -> "Загрузка дорожек..."
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        allDone -> MaterialTheme.colorScheme.primary
                        hasErrors && !isStillUploading -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    uploadItems.forEachIndexed { _, item ->
                        UploadTrackCard(item = item)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (hasErrors && !isStillUploading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Отмена")
                        }
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry")
                        }
                    }
                } else if (!isStillUploading) {
                    TextButton(onClick = onDismiss) {
                        Text("Закрыть")
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadTrackCard(item: UploadItemState) {
    val animatedProgress by animateFloatAsState(
        targetValue = item.progress,
        animationSpec = tween(durationMillis = 400),
        label = "upload_progress"
    )
    val trackColor by animateColorAsState(
        targetValue = when (item.status) {
            UploadStatus.DONE -> MaterialTheme.colorScheme.primary
            UploadStatus.ERROR -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.tertiary
        },
        animationSpec = tween(300),
        label = "track_color"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (item.status) {
                UploadStatus.PENDING, UploadStatus.UPLOADING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                UploadStatus.DONE -> {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                UploadStatus.ERROR -> {
                    Icon(
                        Icons.Filled.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.status != UploadStatus.DONE) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = trackColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
                if (item.status == UploadStatus.ERROR && item.errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.errorMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = when (item.status) {
                    UploadStatus.DONE -> "OK"
                    UploadStatus.ERROR -> "ERR"
                    else -> "${(item.progress * 100).toInt()}%"
                },
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = trackColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
