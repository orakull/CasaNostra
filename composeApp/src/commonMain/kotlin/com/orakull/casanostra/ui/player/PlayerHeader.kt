package com.orakull.casanostra.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun AestheticHeader(
    projectName: String,
    onBack: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
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
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад к проектам",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Удалить проект",
                tint = MaterialTheme.colorScheme.error
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = CircleShape
                    )
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = "Music",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = onRenameClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onBackground
                ),
                border = null,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = projectName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Мультитрек-Сессия",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
