package com.orakull.casanostra

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import casanostra.composeapp.generated.resources.Res
import com.orakull.casanostra.audio.TrackInfo
import com.orakull.casanostra.ui.PlayerScreen
import com.orakull.casanostra.ui.PlayerViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi

val DarkThemeColors = darkColorScheme(
    primary = Color(0xFF00E676),
    onPrimary = Color(0xFF1B1B1F),
    primaryContainer = Color(0xFF1B1B1F),
    onPrimaryContainer = Color(0xFF00E676),
    background = Color(0xFF121215),
    onBackground = Color(0xFFE3E3E8),
    surface = Color(0xFF1B1B1F),
    onSurface = Color(0xFFE3E3E8),
    surfaceVariant = Color(0xFF23232A),
    onSurfaceVariant = Color(0xFFAAAABB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@OptIn(ExperimentalResourceApi::class)
@Composable
fun App() {
    MaterialTheme(colorScheme = DarkThemeColors) {
        val viewModel: PlayerViewModel = viewModel { PlayerViewModel() }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            scope.launch {
                val trackFiles = listOf(
                    "files/bass_vocals.wav" to "Бас (Вокал)",
                    "files/tenor_piano.wav" to "Тенор (Фортепиано)",
                    "files/tenor_vocals.wav" to "Тенор (Вокал)"
                )

                val trackInfos = trackFiles.map { (path, name) ->
                    val bytes = Res.readBytes(path)
                    TrackInfo(name = name, resourceBytes = bytes)
                }

                viewModel.loadTracks(trackInfos)
            }
        }

        PlayerScreen(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )
    }
}