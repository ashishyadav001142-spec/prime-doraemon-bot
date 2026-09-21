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
    }

    private lateinit var prefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSupabaseUrl(): String {
        return prefs.getString(KEY_SUPABASE_URL, "https://gtgmnuuikwfalbxmorov.supabase.co") ?: "https://gtgmnuuikwfalbxmorov.supabase.co"
    }

    fun setSupabaseUrl(url: String) {
        prefs.edit().putString(KEY_SUPABASE_URL, url.trim().trimEnd('/')).apply()
    }

    fun getSupabaseAnonKey(): String {
        return prefs.getString(
            KEY_SUPABASE_ANON_KEY,
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imd0Z21udXVpa3dmYWxieG1vcm92Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk5OTU1NzIsImV4cCI6MjEwNTU3MTU3Mn0.sD5bulYeAVrxMLIKJ55E3euuGJMpRRVJPXG3WpUazCI"
        ) ?: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imd0Z21udXVpa3dmYWxieG1vcm92Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk5OTU1NzIsImV4cCI6MjEwNTU3MTU3Mn0.sD5bulYeAVrxMLIKJ55E3euuGJMpRRVJPXG3WpUazCI"
    }

    fun setSupabaseAnonKey(key: String) {
        prefs.edit().putString(KEY_SUPABASE_ANON_KEY, key.trim()).apply()
    }

    fun getAdminToken(): String {
        return prefs.getString(KEY_ADMIN_TOKEN, "") ?: ""
    }

    fun setAdminToken(token: String) {
        prefs.edit().putString(KEY_ADMIN_TOKEN, token.trim()).apply()
    }

    fun isConfigured(): Boolean {
        return getSupabaseUrl().isNotEmpty() && getSupabaseAnonKey().isNotEmpty()
    }
}
