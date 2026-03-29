package com.orakull.casanostra.storage

// TODO: Implement using NSUserDefaults for iOS.
actual object LocalStorage {
    actual fun get(key: String): String? = null
    actual fun set(key: String, value: String) {}
    actual fun remove(key: String) {}
}
