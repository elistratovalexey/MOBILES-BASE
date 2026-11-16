package com.example.mobilesbase.utils

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

object SyncState {

    private const val FILE = "sync_state"
    private const val KEY_LOCAL_CHANGED = "local_changed_at"
    private const val KEY_LAST_SYNC = "last_sync_at"
    private const val KEY_LAST_RESULT = "last_result"
    private const val KEY_AUTO = "auto_sync_enabled"
    private const val KEY_DEVICE = "device_id"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun localChangedAt(context: Context): Long = prefs(context).getLong(KEY_LOCAL_CHANGED, 0L)

    fun markLocalChanged(context: Context) {
        prefs(context).edit().putLong(KEY_LOCAL_CHANGED, System.currentTimeMillis()).commit()
    }

    fun setLocalChangedAt(context: Context, value: Long) {
        prefs(context).edit().putLong(KEY_LOCAL_CHANGED, value).commit()
    }

    fun lastSyncAt(context: Context): Long = prefs(context).getLong(KEY_LAST_SYNC, 0L)

    fun lastResult(context: Context): String =
        prefs(context).getString(KEY_LAST_RESULT, "Синхронизация ещё не выполнялась") ?: ""

    fun setSyncResult(context: Context, message: String) {
        prefs(context).edit()
            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
            .putString(KEY_LAST_RESULT, message)
            .commit()
    }

    fun autoEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_AUTO, true)

    fun setAutoEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO, value).commit()
    }

    fun deviceId(context: Context): String {
        val p = prefs(context)
        var id = p.getString(KEY_DEVICE, null)
        if (id.isNullOrBlank()) {
            id = UUID.randomUUID().toString().take(8)
            p.edit().putString(KEY_DEVICE, id).commit()
        }
        return id
    }
}
