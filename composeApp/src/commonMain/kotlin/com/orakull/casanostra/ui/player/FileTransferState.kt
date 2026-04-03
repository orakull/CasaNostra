package com.orakull.casanostra.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orakull.casanostra.ui.common.AppError

enum class TransferStatus { PENDING, IN_PROGRESS, DONE, ERROR }

data class FileTransferItemState(
    val name: String,
    val status: TransferStatus = TransferStatus.PENDING,
    val progress: Float = 0f,
    /** Структурированная ошибка: user-friendly сообщение + сырая техническая деталь. */
    val error: AppError? = null
)

@Composable
internal fun FileTransferTrackCard(item: FileTransferItemState) {
    val animatedProgress by animateFloatAsState(
        targetValue = item.progress,
        animationSpec = tween(durationMillis = 400),
        label = "transfer_progress"
    )
    val trackColor by animateColorAsState(
        targetValue = when (item.status) {
            TransferStatus.DONE -> MaterialTheme.colorScheme.primary
            TransferStatus.ERROR -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.tertiary
        },
        animationSpec = tween(300),
        label = "transfer_track_color"
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
                TransferStatus.PENDING, TransferStatus.IN_PROGRESS -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                TransferStatus.DONE -> {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                TransferStatus.ERROR -> {
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
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.status != TransferStatus.DONE) {
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
                // Показываем user-friendly сообщение, а не сырую ошибку
                if (item.status == TransferStatus.ERROR && item.error != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.error.userMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = when (item.status) {
                    TransferStatus.DONE -> "OK"
                    TransferStatus.ERROR -> "ERR"
                    else -> "${(item.progress * 100).toInt()}%"
                },
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = trackColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
