package com.orakull.casanostra.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orakull.casanostra.ui.theme.LocalCasaNostraColors

/**
 * Сегментированный контрол Mute/Solo.
 *
 * M и S — два прямоугольника без зазора:
 * - левая кнопка скруглена только слева (topStart/bottomStart = 8dp)
 * - правая кнопка скруглена только справа (topEnd/bottomEnd = 8dp)
 *
 * Внешние радиусы (8dp) визуально согласуются с внутренней поверхностью
 * карточки трека (cornerRadius=16dp, padding=12dp → inner ≈ 4dp, используем 8dp).
 */
@Composable
internal fun MuteSoloSegmentedButton(
    isMuted: Boolean,
    onMuteToggle: () -> Unit,
    isSolo: Boolean,
    onSoloToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extras = LocalCasaNostraColors.current

    val muteBackground by animateColorAsState(
        targetValue = if (isMuted) extras.muteActive else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(200),
        label = "mute_bg",
    )
    val muteTextColor by animateColorAsState(
        targetValue = if (isMuted) extras.onMuteActive else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "mute_text",
    )
    val soloBackground by animateColorAsState(
        targetValue = if (isSolo) extras.soloActive else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(200),
        label = "solo_bg",
    )
    val soloTextColor by animateColorAsState(
        targetValue = if (isSolo) extras.onSoloActive else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "solo_text",
    )

    Row(modifier = modifier) {
        // M — левая половина
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 32.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                .background(muteBackground)
                .clickable { onMuteToggle() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "M",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = muteTextColor,
            )
        }
        // S — правая половина
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 32.dp)
                .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                .background(soloBackground)
                .clickable { onSoloToggle() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "S",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = soloTextColor,
            )
        }
    }
}
