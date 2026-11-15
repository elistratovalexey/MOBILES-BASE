package com.example.mobilesbase.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecureStorage {
    private fun getPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(context, "cloud_prefs", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    }

    fun saveCredentials(context: Context, email: String, password: String) {
        getPrefs(context).edit().apply { putString("cloud_email", email); putString("cloud_password", password) }.apply()
    }
    fun getEmail(context: Context): String? = getPrefs(context).getString("cloud_email", null)
    fun getPassword(context: Context): String? = getPrefs(context).getString("cloud_password", null)
}
