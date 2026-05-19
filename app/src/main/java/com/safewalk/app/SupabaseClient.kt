package com.safewalk.app

import android.content.Context
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
// Agregamos este import vital:
import com.russhwolf.settings.SharedPreferencesSettings

object SupabaseClient {
    lateinit var client: io.github.jan.supabase.SupabaseClient
        private set

    fun init(context: Context) {
        client = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                // 1. Obtenemos las preferencias nativas
                val prefs = context.getSharedPreferences("safewalk_auth", Context.MODE_PRIVATE)

                // 2. Las envolvemos con SharedPreferencesSettings
                sessionManager = SettingsSessionManager(SharedPreferencesSettings(prefs))
            }
            install(Postgrest)
            install(Realtime)
            install(io.github.jan.supabase.storage.Storage)
        }
    }
}