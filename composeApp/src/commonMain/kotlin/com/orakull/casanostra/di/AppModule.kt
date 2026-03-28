package com.orakull.casanostra.di

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import org.koin.dsl.module
import com.orakull.casanostra.Secrets
import com.orakull.casanostra.cache.createAudioFileCache
import com.orakull.casanostra.data.repository.ProjectRepository
import com.orakull.casanostra.data.repository.TrackRepository

val appModule = module {
    single<SupabaseClient> {
        createSupabaseClient(
            supabaseUrl = Secrets.SUPABASE_URL,
            supabaseKey = Secrets.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage)
        }
    }

    // Platform-specific audio cache (Android: cacheDir, iOS: NSCachesDirectory, Web: in-memory)
    single { createAudioFileCache() }

    single { ProjectRepository(get(), get()) }
    single { TrackRepository(get(), get()) }
}
