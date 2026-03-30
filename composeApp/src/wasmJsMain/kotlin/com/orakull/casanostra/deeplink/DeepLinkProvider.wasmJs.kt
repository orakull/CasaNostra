package com.orakull.casanostra.deeplink

import kotlinx.browser.window

actual fun getInitialDeepLinkToken(): String? {
    val basePath = basePath()
    val path = window.location.pathname // e.g. "/workspace/abc-123-def" or "/CasaNostra/workspace/abc-123-def"
    val prefix = "$basePath/workspace/"
    return if (path.startsWith(prefix)) {
        path.removePrefix(prefix).trimEnd('/').takeIf { it.isNotBlank() }
    } else {
        null
    }
}

actual fun getAppBaseUrl(): String = window.location.origin + basePath()

/** Returns the base path from the <base href> tag, e.g. "" on VPS or "/CasaNostra" on GitHub Pages. */
private fun basePath(): String {
    val href = kotlinx.browser.document.querySelector("base")?.getAttribute("href") ?: "/"
    return href.trimEnd('/')
}
