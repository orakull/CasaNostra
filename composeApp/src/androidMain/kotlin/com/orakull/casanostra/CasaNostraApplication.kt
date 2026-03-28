package com.orakull.casanostra

import android.app.Application
import com.orakull.casanostra.cache.initAndroidAudioCache
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class CasaNostraApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialise the Android audio cache with application context.
        // Must be called before Koin starts so that AudioFileCache is ready
        // when the Koin graph is built.
        initAndroidAudioCache(this)

        startKoin {
            androidContext(this@CasaNostraApplication)
            modules(supabaseModule)
        }
    }
}
