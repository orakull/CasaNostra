package com.orakull.casanostra.storage

import kotlinx.browser.localStorage

actual object LocalStorage {
    actual fun get(key: String): String? = localStorage.getItem(key)
    actual fun set(key: String, value: String) { localStorage.setItem(key, value) }
    actual fun remove(key: String) { localStorage.removeItem(key) }
}
