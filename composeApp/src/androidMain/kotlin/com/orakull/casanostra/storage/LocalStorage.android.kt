package com.orakull.casanostra.storage

import android.content.Context
import android.content.SharedPreferences

private lateinit var prefs: SharedPreferences

/** Called once from [com.orakull.casanostra.CasaNostraApplication.onCreate]. */
fun initAndroidLocalStorage(context: Context) {
    prefs = context.applicationContext
        .getSharedPreferences("casanostra_prefs", Context.MODE_PRIVATE)
}

actual object LocalStorage {
    actual fun get(key: String): String? = prefs.getString(key, null)
    actual fun set(key: String, value: String) { prefs.edit().putString(key, value).apply() }
    actual fun remove(key: String) { prefs.edit().remove(key).apply() }
}
