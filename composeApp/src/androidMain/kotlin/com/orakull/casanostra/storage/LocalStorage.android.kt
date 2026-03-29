package com.orakull.casanostra.storage

// TODO: Implement using SharedPreferences for Android.
// SharedPreferences requires Context — wire via Koin or application-level singleton when implementing.
actual object LocalStorage {
    actual fun get(key: String): String? = null
    actual fun set(key: String, value: String) {}
    actual fun remove(key: String) {}
}
