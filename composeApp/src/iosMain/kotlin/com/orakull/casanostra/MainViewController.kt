package com.orakull.casanostra

import androidx.compose.ui.window.ComposeUIViewController
import com.orakull.casanostra.di.appModule
import org.koin.core.context.startKoin

private var koinStarted = false

fun MainViewController() = ComposeUIViewController {
    if (!koinStarted) {
        startKoin {
            modules(appModule)
        }
        koinStarted = true
    }
    App()
}