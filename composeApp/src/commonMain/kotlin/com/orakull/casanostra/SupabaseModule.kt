package com.orakull.casanostra

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import org.koin.dsl.module

val supabaseModule = module {
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
    
    single { com.orakull.casanostra.data.repository.ProjectRepository(get()) }
    single { com.orakull.casanostra.data.repository.TrackRepository(get()) }
}