package com.example.mobilesbase.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilesbase.data.MobileCard
import com.example.mobilesbase.data.MobileDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    fun updateBalanceManually(cardId: Int, newBalance: Double) = viewModelScope.launch {
        val cards = allCards.value
        val card = cards.find { it.id == cardId }
        if (card != null) {
            val updatedCard = card.copy(
                balance = newBalance,
                lastUpdated = System.currentTimeMillis()
            )
            dao.updateCard(updatedCard)
        }
    }
}
