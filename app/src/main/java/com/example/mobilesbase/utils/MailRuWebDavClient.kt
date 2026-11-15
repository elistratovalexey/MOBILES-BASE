package com.example.mobilesbase.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class MailRuWebDavClient(private val email: String, private val appPassword: String) {
    private val client = OkHttpClient()
    private val webDavUrl = "https://webdav.cloud.mail.ru/MobilesBase/backup.json"
    private val credentials = Credentials.basic(email, appPassword)

    suspend fun uploadJson(jsonData: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(webDavUrl).header("Authorization", credentials)
                .put(jsonData.toRequestBody("application/json".toMediaType())).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful || response.code == 201 || response.code == 204) Result.success(Unit)
            else Result.failure(Exception("Ошибка загрузки: ${response.code} ${response.message}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun downloadJson(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(webDavUrl).header("Authorization", credentials).get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) Result.success(response.body?.string() ?: "")
            else Result.failure(Exception("Ошибка скачивания: ${response.code}"))
        } catch (e: Exception) { Result.failure(e) }
    }
}
