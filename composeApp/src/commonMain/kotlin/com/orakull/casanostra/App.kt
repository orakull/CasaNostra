package com.orakull.casanostra

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import casanostra.composeapp.generated.resources.Res
import com.orakull.casanostra.audio.TrackInfo
import com.orakull.casanostra.ui.PlayerScreen
import com.orakull.casanostra.ui.PlayerViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun App() {
    MaterialTheme {
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
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize()
        )
    }
}