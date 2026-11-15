package com.example.mobilesbase.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilesbase.data.MobileCard
import com.example.mobilesbase.data.MobileDatabase
import com.example.mobilesbase.utils.MailRuWebDavClient
import com.example.mobilesbase.utils.SecureStorage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MobileViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = MobileDatabase.getDatabase(application).mobileDao()
    private val context = application.applicationContext
    private val gson = Gson()

    val allCards = dao.getAllCards().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun insertCard(card: MobileCard) = viewModelScope.launch { dao.insertCard(card) }
    fun updateCard(card: MobileCard) = viewModelScope.launch { dao.updateCard(card) }
    fun deleteCard(card: MobileCard) = viewModelScope.launch { dao.deleteCard(card) }

    fun updateBalanceManually(cardId: Int, newBalance: Double) = viewModelScope.launch {
        val card = allCards.value.find { it.id == cardId }
        if (card != null) dao.updateCard(card.copy(balance = newBalance, lastUpdated = System.currentTimeMillis()))
    }

    fun saveCloudCredentials(email: String, password: String) { SecureStorage.saveCredentials(context, email, password) }

    fun syncToCloud(onResult: (String) -> Unit) = viewModelScope.launch {
        val email = SecureStorage.getEmail(context) ?: run { onResult("Нет данных для входа"); return@launch }
        val password = SecureStorage.getPassword(context) ?: run { onResult("Нет данных для входа"); return@launch }
        val json = gson.toJson(mapOf("cards" to allCards.value))
        val result = MailRuWebDavClient(email, password).uploadJson(json)
        result.fold({ onResult("✅ Успешно сохранено в облако!") }, { onResult("❌ Ошибка: ${it.message}") })
    }

    fun syncFromCloud(onResult: (String) -> Unit) = viewModelScope.launch {
        val email = SecureStorage.getEmail(context) ?: run { onResult("Нет данных для входа"); return@launch }
        val password = SecureStorage.getPassword(context) ?: run { onResult("Нет данных для входа"); return@launch }
        val result = MailRuWebDavClient(email, password).downloadJson()
        result.fold({ json ->
            try {
                val type = object : TypeToken<Map<String, List<MobileCard>>>() {}.type
                val cloudCards = (gson.fromJson(json, type) as? Map<*, *>)?.get("cards") as? List<MobileCard> ?: emptyList()
                cloudCards.forEach { card ->
                    val existing = allCards.value.find { it.phoneNumber == card.phoneNumber }
                    if (existing != null) dao.updateCard(card.copy(id = existing.id)) else dao.insertCard(card.copy(id = 0))
                }
                onResult("✅ Восстановлено! (${cloudCards.size} записей)")
            } catch (e: Exception) { onResult("❌ Ошибка чтения: ${e.message}") }
        }, { onResult("❌ Ошибка: ${it.message}") })
    }
}
