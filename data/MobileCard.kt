package com.example.mobilesbase.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mobile_cards")
data class MobileCard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phoneNumber: String,       // Формат: +7 xxx xxx-xx-xx
    val operator: String,          // Название оператора
    val monthlyPayment: Double,    // Сумма оплаты в месяц
    val monthlyMinutes: String,    // "500" или "Безлимит"
    val monthlyGb: String,         // "15" или "Безлимит"
    val simLocation: String,       // Где стоит (напр. "Телефон, Слот 1")
    val balance: Double = 0.0      // Остаток денег
)
