package com.example.schoolproject.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseProvider {

    // Public project configuration. Database access is enforced by authenticated RLS policies.
    private const val SUPABASE_URL = "https://ehnwfbbpzozdncuigoov.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVobndmYmJwem96ZG5jdWlnb292Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTQ3NTUyMTEsImV4cCI6MjExMDMzMTIxMX0.wsMK3vFCTdNrZFAiCL68ovrrcNR8wDyw-H4eBGtGKhI"

    val client by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            install(Auth) {
                autoLoadFromStorage = true
                autoSaveToStorage = true
            }
        }
    }
}
