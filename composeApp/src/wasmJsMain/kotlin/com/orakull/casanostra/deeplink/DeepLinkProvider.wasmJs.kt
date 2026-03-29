package com.orakull.casanostra.deeplink

import kotlinx.browser.window

actual fun getInitialDeepLinkToken(): String? {
    val path = window.location.pathname // e.g. "/workspace/abc-123-def"
    val prefix = "/workspace/"
    return if (path.startsWith(prefix)) {
        path.removePrefix(prefix).trimEnd('/').takeIf { it.isNotBlank() }
    } else {
        null
    }
}

actual fun getAppBaseUrl(): String = window.location.origin
