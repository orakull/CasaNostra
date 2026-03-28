package com.orakull.casanostra.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun TrackRow(
    track: TrackState,
    index: Int,
    onVolumeChange: (Float) -> Unit,
    onMuteToggle: () -> Unit,
    onSoloToggle: () -> Unit,
    onTrackClick: (() -> Unit)? = {},
    modifier: Modifier = Modifier
) {
    val isMuted = track.isMuted
    val isSolo = track.isSolo

    Card(
        onClick = { onTrackClick?.invoke() },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        enabled = onTrackClick != null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top row: color indicator + name + volume % + M/S buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(track.color)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${(track.volume * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                LetterToggleButton(
                    text = "M",
                    isActive = isMuted,
                    activeColor = Color(0xFF5B7AA5),
                    onClick = onMuteToggle
                )
                Spacer(modifier = Modifier.width(6.dp))
                LetterToggleButton(
                    text = "S",
                    isActive = isSolo,
                    activeColor = Color(0xFFC49A3C),
                    onClick = onSoloToggle
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom row: volume icon + slider
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (track.volume == 0f || isMuted) Icons.AutoMirrored.Filled.VolumeOff
                    else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = track.volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f).height(20.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = track.color,
                        activeTrackColor = track.color,
                        inactiveTrackColor = track.color.copy(alpha = 0.15f)
                    )
                )
            }
        }
    }
}

@Composable
private fun LetterToggleButton(
    text: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) activeColor else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(200)
    )
    val textColor by animateColorAsState(
        targetValue = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200)
    )

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(backgroundColor)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
