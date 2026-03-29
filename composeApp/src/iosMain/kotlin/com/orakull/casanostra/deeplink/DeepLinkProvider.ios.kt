package com.orakull.casanostra.deeplink

// TODO: Implement iOS Universal Links — extract token from the URL passed to
// application(_:continue:restorationHandler:) in AppDelegate / SceneDelegate.
actual fun getInitialDeepLinkToken(): String? = null

actual fun getAppBaseUrl(): String = "https://casanostra.orakull.ru"
