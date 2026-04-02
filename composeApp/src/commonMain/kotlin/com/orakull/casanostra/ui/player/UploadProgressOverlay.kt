package com.orakull.casanostra.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private fun UploadItemState.toTransferItem() = FileTransferItemState(
    name = file.name,
    status = when (status) {
        UploadStatus.PENDING   -> TransferStatus.PENDING
        UploadStatus.UPLOADING -> TransferStatus.IN_PROGRESS
        UploadStatus.DONE      -> TransferStatus.DONE
        UploadStatus.ERROR     -> TransferStatus.ERROR
    },
    progress = progress,
    errorMessage = errorMessage
)

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
                    uploadItems.forEach { item ->
                        FileTransferTrackCard(item = item.toTransferItem())
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
