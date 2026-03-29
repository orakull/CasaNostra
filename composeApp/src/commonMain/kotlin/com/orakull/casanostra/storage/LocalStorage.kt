package com.orakull.casanostra.storage

/**
 * Simple persistent key-value storage, platform-specific.
 *
 * Web: browser localStorage.
 * Android: SharedPreferences (future implementation, no-op for now).
 * iOS: NSUserDefaults (future implementation, no-op for now).
 */
expect object LocalStorage {
    fun get(key: String): String?
    fun set(key: String, value: String)
    fun remove(key: String)
}

/** Key for storing a pending workspace share token across page refreshes / app restarts. */
const val KEY_PENDING_SHARE_TOKEN = "casanostra_pending_share_token"
