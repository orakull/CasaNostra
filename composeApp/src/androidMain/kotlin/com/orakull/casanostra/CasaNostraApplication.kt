package com.orakull.casanostra

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class CasaNostraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidContext(this@CasaNostraApplication)
            modules(supabaseModule)
        }
    }
}
