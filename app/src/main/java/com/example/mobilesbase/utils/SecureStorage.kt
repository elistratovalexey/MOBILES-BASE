package com.example.mobilesbase.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecureStorage {

    private const val FILE_NAME = "cloud_prefs"
    private const val KEY_EMAIL = "cloud_email"
    private const val KEY_PASSWORD = "cloud_password"

    @Volatile
    private var cached: SharedPreferences? = null

    private fun prefs(context: Context): SharedPreferences {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val app = context.applicationContext
            val created = try {
                build(app)
            } catch (e: Exception) {
                app.deleteSharedPreferences(FILE_NAME)
                build(app)
            }
            cached = created
            return created
        }
    }

    private fun build(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveCredentials(context: Context, email: String, password: String) {
        prefs(context).edit()
            .putString(KEY_EMAIL, email.trim())
            .putString(KEY_PASSWORD, password.trim())
            .commit()
    }

    fun getEmail(context: Context): String? = prefs(context).getString(KEY_EMAIL, null)

    fun getPassword(context: Context): String? = prefs(context).getString(KEY_PASSWORD, null)

    fun hasCredentials(context: Context): Boolean =
        !getEmail(context).isNullOrBlank() && !getPassword(context).isNullOrBlank()

    fun clear(context: Context) {
        prefs(context).edit().clear().commit()
    }
}
