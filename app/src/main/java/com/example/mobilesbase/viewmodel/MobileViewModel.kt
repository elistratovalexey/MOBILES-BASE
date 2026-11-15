package com.example.mobilesbase.viewmodel

import android.app.Application
import android.content.Context
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
        if (card != null) {
            dao.updateCard(card.copy(balance = newBalance, lastUpdated = System.currentTimeMillis()))
        }
    }

    // --- ЛОГИКА СИНХРОНИЗАЦИИ ---

    fun saveCloudCredentials(email: String, password: String) {
        SecureStorage.saveCredentials(context, email, password)
    }

    fun syncToCloud(onResult: (String) -> Unit) = viewModelScope.launch {
        val email = SecureStorage.getEmail(context) ?: run { onResult("Нет сохраненных данных для входа"); return@launch }
        val password = SecureStorage.getPassword(context) ?: run { onResult("Нет сохраненных данных для входа"); return@launch }

        val cards = allCards.value
        val json = gson.toJson(mapOf("cards" to cards))
        
        val client = MailRuWebDavClient(email, password)
        val result = client.uploadJson(json)
        
        result.fold(
            onSuccess = { onResult("✅ Успешно сохранено в облако!") },
            onFailure = { onResult("❌ Ошибка: ${it.message}") }
        )
    }

    fun syncFromCloud(onResult: (String) -> Unit) = viewModelScope.launch {
        val email = SecureStorage.getEmail(context) ?: run { onResult("Нет сохраненных данных для входа"); return@launch }
        val password = SecureStorage.getPassword(context) ?: run { onResult("Нет сохраненных данных для входа"); return@launch }

        val client = MailRuWebDavClient(email, password)
        val result = client.downloadJson()
        
        result.fold(
            onSuccess = { json ->
                try {
                    val type = object : TypeToken<Map<String, List<MobileCard>>>() {}.type
                    val data: Map<String, List<MobileCard>> = gson.fromJson(json, type)
                    val cloudCards = data["cards"] ?: emptyList()
                    
                    // OnConflictStrategy.REPLACE автоматически обновит существующие и добавит новые
                    cloudCards.forEach { card -> 
                        // Обнуляем ID, чтобы Room сам решил: обновить по другим полям или создать новый, 
                        // но лучше искать по номеру телефона. Для простоты используем REPLACE по ID, 
                        // но при импорте ID могут не совпасть. 
                        // Лучший способ: найти существующий по номеру и обновить его.
                        val existing = allCards.value.find { it.phoneNumber == card.phoneNumber }
                        if (existing != null) {
                            dao.updateCard(card.copy(id = existing.id)) // Сохраняем локальный ID
                        } else {
                            dao.insertCard(card.copy(id = 0)) // Вставляем как новый
                        }
                    }
                    onResult("✅ Успешно восстановлено из облака! (${cloudCards.size} записей)")
                } catch (e: Exception) {
                    onResult("❌ Ошибка чтения данных: ${e.message}")
                }
            },
            onFailure = { onResult("❌ Ошибка: ${it.message}") }
        )
    }
}
