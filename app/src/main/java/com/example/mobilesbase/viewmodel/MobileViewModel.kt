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
        if (card != null) {
            dao.updateCard(card.copy(balance = newBalance, lastUpdated = System.currentTimeMillis()))
        }
    }

    fun saveCloudCredentials(email: String, password: String) {
        SecureStorage.saveCredentials(context, email.trim(), password.trim())
    }

    private fun credentials(): Pair<String, String>? {
        val email = SecureStorage.getEmail(context)
        val password = SecureStorage.getPassword(context)
        return if (email.isNullOrBlank() || password.isNullOrBlank()) null else email to password
    }

    fun syncToCloud(onResult: (String) -> Unit) = viewModelScope.launch {
        val creds = credentials()
        if (creds == null) {
            onResult("❌ Введите email и пароль, поставьте галочку «Сохранить данные»")
            return@launch
        }
        val cards = allCards.value
        if (cards.isEmpty()) {
            onResult("❌ База пуста — облачная копия НЕ перезаписана")
            return@launch
        }
        val json = gson.toJson(mapOf("cards" to cards))
        val result = MailRuWebDavClient(creds.first, creds.second).uploadJson(json)
        result.fold(
            { onResult("✅ В облако выгружено записей: ${cards.size}") },
            { onResult("❌ ${it.message}") }
        )
    }

    fun syncFromCloud(onResult: (String) -> Unit) = viewModelScope.launch {
        val creds = credentials()
        if (creds == null) {
            onResult("❌ Введите email и пароль, поставьте галочку «Сохранить данные»")
            return@launch
        }
        val result = MailRuWebDavClient(creds.first, creds.second).downloadJson()
        result.fold({ json ->
            try {
                val type = object : TypeToken<Map<String, List<MobileCard>>>() {}.type
                val parsed: Map<String, List<MobileCard>>? = gson.fromJson(json, type)
                val cloudCards = parsed?.get("cards") ?: emptyList()
                if (cloudCards.isEmpty()) {
                    onResult("❌ В облаке пустая копия")
                    return@fold
                }
                val local = allCards.value.associateBy { it.phoneNumber }
                var added = 0
                var updated = 0
                cloudCards.forEach { card ->
                    val existing = local[card.phoneNumber]
                    if (existing != null) {
                        dao.updateCard(card.copy(id = existing.id))
                        updated++
                    } else {
                        dao.insertCard(card.copy(id = 0))
                        added++
                    }
                }
                onResult("✅ Готово: добавлено $added, обновлено $updated")
            } catch (e: Exception) {
                onResult("❌ Ошибка чтения копии: ${e.message}")
            }
        }, { onResult("❌ ${it.message}") })
    }
}
