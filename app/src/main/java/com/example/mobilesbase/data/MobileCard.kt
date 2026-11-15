package com.example.mobilesbase.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mobile_cards")
data class MobileCard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phoneNumber: String,
    val operator: String,
    val monthlyPayment: Double,
    val monthlyMinutes: String,
    val monthlyGb: String,
    val simLocation: String,
    val balance: Double = 0.0,
    val lastUpdated: Long = 0L // Timestamp последнего обновления
)
