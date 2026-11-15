package com.example.mobilesbase.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilesbase.data.MobileCard
import com.example.mobilesbase.data.MobileDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class MobileViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = MobileDatabase.getDatabase(application).mobileDao()

    val allCards = dao.getAllCards().stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        emptyList()
    )

    fun insertCard(card: MobileCard) = viewModelScope.launch { dao.insertCard(card) }
    fun updateCard(card: MobileCard) = viewModelScope.launch { dao.updateCard(card) }
    fun deleteCard(card: MobileCard) = viewModelScope.launch { dao.deleteCard(card) }

    /**
     * ЗАГЛУШКА для автоматического получения баланса.
     * Для реальной работы здесь должен быть HTTP-клиент (Retrofit) с логикой авторизации 
     * конкретного оператора, что требует хранения учетных данных и обхода защит.
     */
    fun fetchBalanceAutomatically(phoneNumber: String) = viewModelScope.launch {
        // Имитация задержки сети и получения данных
        kotlinx.coroutines.delay(1000) 
        val mockBalance = BigDecimal.valueOf(Math.random() * 1000)
            .setScale(2, RoundingMode.HALF_UP).toDouble()
        dao.updateBalance(phoneNumber, mockBalance)
    }
}
