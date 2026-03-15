package com.orakull.casanostra

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import org.koin.core.context.GlobalContext.startKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin {
        modules(supabaseModule)
    }

    ComposeViewport(document.body!!) {
        App()
    }
}
