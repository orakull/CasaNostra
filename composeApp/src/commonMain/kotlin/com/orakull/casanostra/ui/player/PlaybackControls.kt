package com.orakull.casanostra.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun TransportControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onRewind: () -> Unit,
    onStop: () -> Unit,
    isLoaded: Boolean,
    modifier: Modifier = Modifier,
    playButtonSize: Dp = 56.dp,
    playIconSize: Dp = 28.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onRewind,
            enabled = isLoaded,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.FastRewind,
                contentDescription = "Перемотка",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FilledIconButton(
            onClick = onPlayPause,
            enabled = isLoaded,
            modifier = Modifier.size(playButtonSize),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Пауза" else "Воспроизведение",
                modifier = Modifier.size(playIconSize)
            )
        }

        IconButton(
            onClick = onStop,
            enabled = isLoaded,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Stop,
                contentDescription = "Стоп",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun ProgressBar(
    currentPositionMs: Long,
    durationMs: Long,
    isLoaded: Boolean,
    onSeek: (Long) -> Unit,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val sliderValue = if (durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    if (isLandscape) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = formatTime(currentPositionMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SliderWrapper(sliderValue, durationMs, onSeek, isLoaded, Modifier.weight(1f))
            Text(
                text = formatTime(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Column(modifier = modifier) {
            SliderWrapper(sliderValue, durationMs, onSeek, isLoaded, Modifier.fillMaxWidth())
            Row(
                modifier = Modifier.fillMaxWidth().offset(y = (-4).dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPositionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTime(durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SliderWrapper(
    value: Float,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    isLoaded: Boolean,
    modifier: Modifier = Modifier
) {
    Slider(
        value = value,
        onValueChange = { fraction ->
            onSeek((fraction * durationMs).toLong())
        },
        enabled = isLoaded,
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier.height(24.dp)
    )
}

internal fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val secStr = if (seconds < 10) "0$seconds" else "$seconds"
    return "$minutes:$secStr"
}
