package com.orakull.casanostra.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Верхняя строка: кликабельная (цвет + название + % + M/S)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (onTrackClick != null)
                            Modifier.clickable { onTrackClick() }
                        else
                            Modifier,
                    )
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Цветовой индикатор
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(track.color),
                )
                Spacer(modifier = Modifier.width(10.dp))
                // Название трека
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Процент громкости — без фиксированной ширины
                Text(
                    text = "${(track.volume * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Сегментированный контрол M/S (вынесен в MuteSoloButton.kt)
                MuteSoloSegmentedButton(
                    isMuted = track.isMuted,
                    onMuteToggle = onMuteToggle,
                    isSolo = track.isSolo,
                    onSoloToggle = onSoloToggle,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Нижняя строка: иконка громкости + слайдер (не кликабельна)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (track.volume == 0f || track.isMuted)
                        Icons.AutoMirrored.Filled.VolumeOff
                    else
                        Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
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
                        inactiveTrackColor = track.color.copy(alpha = 0.15f),
                    ),
                )
            }
        }
    }
}
