package com.example.mobilesbase.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilesbase.data.MobileCard
import com.example.mobilesbase.data.MobileDatabase
import com.example.mobilesbase.utils.MailRuWebDavClient
import com.example.mobilesbase.utils.SecureStorage
import com.example.mobilesbase.utils.SyncState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class MobileViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = MobileDatabase.getDatabase(application).mobileDao()
    private val context = application.applicationContext
    private val gson = Gson()

    val allCards = dao.getAllCards().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val syncMutex = Mutex()
    private var autoJob: Job? = null

    private val _syncStatus = MutableStateFlow(SyncState.lastResult(context))
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private data class CloudPayload(val updatedAt: Long, val cards: List<MobileCard>)

    private suspend fun snapshot(): List<MobileCard> = dao.getAllCards().first()

    fun insertCard(card: MobileCard) = viewModelScope.launch {
        dao.insertCard(card)
        SyncState.markLocalChanged(context)
    }

    fun updateCard(card: MobileCard) = viewModelScope.launch {
        dao.updateCard(card)
        SyncState.markLocalChanged(context)
    }

    fun deleteCard(card: MobileCard) = viewModelScope.launch {
        dao.deleteCard(card)
        SyncState.markLocalChanged(context)
    }

    fun updateBalanceManually(cardId: Int, newBalance: Double) = viewModelScope.launch {
        val card = snapshot().find { it.id == cardId }
        if (card != null) {
            dao.updateCard(card.copy(balance = newBalance, lastUpdated = System.currentTimeMillis()))
            SyncState.markLocalChanged(context)
        }
    }

    fun saveCloudCredentials(email: String, password: String) {
        SecureStorage.saveCredentials(context, email.trim(), password.trim())
    }

    fun setAutoSync(enabled: Boolean) = SyncState.setAutoEnabled(context, enabled)

    fun startAutoSync() {
        if (autoJob?.isActive == true) return
        autoJob = viewModelScope.launch {
            delay(5000L)
            while (isActive) {
                if (SyncState.autoEnabled(context) && SecureStorage.hasCredentials(context)) {
                    runSync()
                }
                delay(15L * 60L * 1000L)
            }
        }
    }

    fun syncNow(onResult: (String) -> Unit) = viewModelScope.launch {
        onResult(runSync())
    }

    private suspend fun runSync(): String {
        if (!syncMutex.tryLock()) return "Синхронизация уже выполняется"
        return try {
            _syncing.value = true
            val message = doSync()
            SyncState.setSyncResult(context, message)
            _syncStatus.value = message
            message
        } finally {
            _syncing.value = false
            syncMutex.unlock()
        }
    }

    private fun buildPayload(cards: List<MobileCard>, at: Long): String = gson.toJson(
        mapOf(
            "updatedAt" to at,
            "deviceId" to SyncState.deviceId(context),
            "cards" to cards
        )
    )

    private fun parsePayload(json: String): CloudPayload? = try {
        val mapType = object : TypeToken<Map<String, Any>>() {}.type
        val map: Map<String, Any> = gson.fromJson(json, mapType)
        val at = (map["updatedAt"] as? Double)?.toLong() ?: 0L
        val cardsType = object : TypeToken<List<MobileCard>>() {}.type
        val cards: List<MobileCard> =
            gson.fromJson(gson.toJson(map["cards"]), cardsType) ?: emptyList()
        CloudPayload(at, cards)
    } catch (e: Exception) {
        null
    }

    private suspend fun applyCloud(cloud: CloudPayload) {
        val local = snapshot().associateBy { it.phoneNumber }
        cloud.cards.forEach { card ->
            val existing = local[card.phoneNumber]
            if (existing != null) dao.updateCard(card.copy(id = existing.id))
            else dao.insertCard(card.copy(id = 0))
        }
        SyncState.setLocalChangedAt(context, cloud.updatedAt)
    }

    private suspend fun doSync(): String {
        val email = SecureStorage.getEmail(context)
        val password = SecureStorage.getPassword(context)
        if (email.isNullOrBlank() || password.isNullOrBlank()) {
            return "❌ Не заданы email и пароль Облака"
        }

        val client = MailRuWebDavClient(email, password)
        val local = snapshot()
        val localAt = SyncState.localChangedAt(context)

        val download = client.downloadJson()
        if (download.isFailure) return "❌ ${download.exceptionOrNull()?.message}"
        val json = download.getOrNull()

        if (json.isNullOrBlank()) {
            if (local.isEmpty()) return "ℹ️ Облако и база пусты — нечего синхронизировать"
            val now = System.currentTimeMillis()
            val up = client.uploadJson(buildPayload(local, now))
            return if (up.isSuccess) {
                SyncState.setLocalChangedAt(context, now)
                "✅ Облачная копия создана: ${local.size} записей"
            } else "❌ ${up.exceptionOrNull()?.message}"
        }

        val cloud = parsePayload(json) ?: return "❌ Не удалось разобрать облачную копию"

        if (local.isEmpty() && cloud.cards.isNotEmpty()) {
            applyCloud(cloud)
            return "⬇️ База была пуста — загружено из облака: ${cloud.cards.size}"
        }

        if (cloud.cards.isEmpty() && local.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val up = client.uploadJson(buildPayload(local, now))
            return if (up.isSuccess) {
                SyncState.setLocalChangedAt(context, now)
                "⬆️ Облако было пусто — выгружено: ${local.size}"
            } else "❌ ${up.exceptionOrNull()?.message}"
        }

        if (localAt == 0L && local.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val up = client.uploadJson(buildPayload(local, now))
            return if (up.isSuccess) {
                SyncState.setLocalChangedAt(context, now)
                "⬆️ Первая синхронизация — выгружено: ${local.size}"
            } else "❌ ${up.exceptionOrNull()?.message}"
        }

        return when {
            cloud.updatedAt > localAt -> {
                applyCloud(cloud)
                "⬇️ Облако новее — загружено: ${cloud.cards.size}"
            }
            localAt > cloud.updatedAt -> {
                val up = client.uploadJson(buildPayload(local, localAt))
                if (up.isSuccess) "⬆️ Устройство новее — выгружено: ${local.size}"
                else "❌ ${up.exceptionOrNull()?.message}"
            }
            else -> "ℹ️ Данные одинаковые — изменений нет"
        }
    }
}
