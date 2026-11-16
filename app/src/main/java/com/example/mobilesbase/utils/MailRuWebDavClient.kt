package com.example.mobilesbase.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class MailRuWebDavClient(email: String, appPassword: String) {

    private val auth: String = Credentials.basic(email.trim(), appPassword.trim())

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .addNetworkInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Authorization", auth)
                .build()
            chain.proceed(request)
        }
        .build()

    private val folderUrl = "https://webdav.cloud.mail.ru/MobilesBase/"
    private val fileUrl = "https://webdav.cloud.mail.ru/MobilesBase/backup.json"

    private fun exec(request: Request): Pair<Int, String> {
        client.newCall(request).execute().use { response ->
            val text = try { response.body?.string() ?: "" } catch (e: Exception) { "" }
            return response.code to text
        }
    }

    private fun authError(code: Int): String =
        "Доступ запрещён ($code). Проверьте email и ПАРОЛЬ ДЛЯ ВНЕШНЕГО ПРИЛОЖЕНИЯ."

    private fun ensureFolder(): String? {
        val request = Request.Builder()
            .url(folderUrl)
            .method("MKCOL", "".toRequestBody(null))
            .build()
        val (code, body) = exec(request)
        return when (code) {
            in 200..299, 405 -> null
            401, 403 -> authError(code)
            else -> "Не удалось создать папку MobilesBase ($code): ${body.take(150)}"
        }
    }

    suspend fun uploadJson(jsonData: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val folderError = ensureFolder()
            if (folderError != null) return@withContext Result.failure(Exception(folderError))
            val request = Request.Builder()
                .url(fileUrl)
                .put(jsonData.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            val (code, body) = exec(request)
            when (code) {
                in 200..299 -> Result.success(Unit)
                401, 403 -> Result.failure(Exception(authError(code)))
                409 -> Result.failure(Exception("Папка MobilesBase не найдена (409)"))
                507 -> Result.failure(Exception("В Облаке нет свободного места (507)"))
                else -> Result.failure(Exception("Ошибка загрузки ($code): ${body.take(150)}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Сеть: ${e.javaClass.simpleName} — ${e.message}"))
        }
    }

    /** null означает, что файла в облаке ещё нет (404). */
    suspend fun downloadJson(): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(fileUrl).get().build()
            val (code, body) = exec(request)
            when (code) {
                in 200..299 -> Result.success(body)
                404 -> Result.success(null)
                401, 403 -> Result.failure(Exception(authError(code)))
                else -> Result.failure(Exception("Ошибка скачивания ($code): ${body.take(150)}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Сеть: ${e.javaClass.simpleName} — ${e.message}"))
        }
    }
}
