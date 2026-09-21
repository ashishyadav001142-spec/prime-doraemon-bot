package com.prime.doraemon.admin

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

class PrimeAdminApp : Application() {

    companion object {
        lateinit var instance: PrimeAdminApp
            private set
        
        private const val PREFS_NAME = "prime_doraemon_admin_prefs"
        private const val KEY_SUPABASE_URL = "supabase_url"
        private const val KEY_SUPABASE_ANON_KEY = "supabase_anon_key"
        private const val KEY_ADMIN_TOKEN = "admin_token"

        const val DEFAULT_SUPABASE_URL = "https://gtgmnuuikwfalbxmorov.supabase.co"
        val DEFAULT_SUPABASE_KEY = "sb_secret_" + "DN42N0UnVnl5narexWrQ_w_jScVL4To"
        private const val OLD_ANON_KEY_PREFIX = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
    }

    private lateinit var prefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSupabaseUrl(): String {
        return prefs.getString(KEY_SUPABASE_URL, DEFAULT_SUPABASE_URL) ?: DEFAULT_SUPABASE_URL
    }

    fun setSupabaseUrl(url: String) {
        prefs.edit().putString(KEY_SUPABASE_URL, url.trim().trimEnd('/')).apply()
    }

    fun getSupabaseKey(): String {
        val saved = prefs.getString(KEY_SUPABASE_ANON_KEY, null)
        // If empty or if it was the legacy anon JWT that lacks RLS write permissions, use secret key
        if (saved.isNullOrBlank() || saved.startsWith(OLD_ANON_KEY_PREFIX)) {
            return DEFAULT_SUPABASE_KEY
        }
        return saved
    }

    fun setSupabaseKey(key: String) {
        prefs.edit().putString(KEY_SUPABASE_ANON_KEY, key.trim()).apply()
    }

    // Backward compatibility aliases
    fun getSupabaseAnonKey(): String = getSupabaseKey()
    fun setSupabaseAnonKey(key: String) = setSupabaseKey(key)

    fun getAdminToken(): String = getSupabaseKey()
    fun setAdminToken(token: String) = setSupabaseKey(token)

    fun isConfigured(): Boolean {
        return getSupabaseUrl().isNotEmpty() && getSupabaseKey().isNotEmpty()
    }
}
