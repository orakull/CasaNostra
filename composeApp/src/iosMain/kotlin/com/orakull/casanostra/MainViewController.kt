package com.orakull.casanostra

import androidx.compose.ui.window.ComposeUIViewController
import org.koin.core.context.startKoin

private var koinStarted = false

fun MainViewController() = ComposeUIViewController {
    if (!koinStarted) {
        startKoin {
            modules(supabaseModule)
        }
        koinStarted = true
    }
    App()
}