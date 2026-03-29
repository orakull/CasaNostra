package com.orakull.casanostra.deeplink

/**
 * Provides the initial deep-link token when the app starts.
 *
 * Web: extracted from window.location.pathname (/workspace/{token}).
 * Android: extracted from the launch Intent URI (future implementation).
 * iOS: extracted from the universal link URL (future implementation).
 *
 * Returns null if the app was not opened via a workspace share link.
 */
expect fun getInitialDeepLinkToken(): String?
