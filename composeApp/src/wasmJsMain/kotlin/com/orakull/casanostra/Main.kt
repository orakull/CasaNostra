package com.orakull.casanostra

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.orakull.casanostra.deeplink.getInitialDeepLinkToken
import com.orakull.casanostra.di.appModule
import com.orakull.casanostra.storage.KEY_PENDING_SHARE_TOKEN
import com.orakull.casanostra.storage.LocalStorage
import kotlinx.browser.document
import org.koin.core.context.GlobalContext.startKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin {
        modules(appModule)
    }

    // Token priority: URL path (first visit) > localStorage (page refresh)
    val urlToken = getInitialDeepLinkToken()
    val storedToken = LocalStorage.get(KEY_PENDING_SHARE_TOKEN)
    val initialToken: String? = urlToken ?: storedToken

    // Persist token from URL so it survives page refresh
    if (urlToken != null) {
        LocalStorage.set(KEY_PENDING_SHARE_TOKEN, urlToken)
    }

    ComposeViewport(document.body!!) {
        App(initialDeepLinkToken = initialToken)
    }
}
