package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "holdings")
data class Holding(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val name: String,
    val quantity: Double,
    val averagePrice: Double,
    val category: String // "STOCKS", "FUNDS", "RETIREMENT"
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val name: String,
    val quantity: Double,
    val price: Double,
    val type: String, // "BUY", "SELL"
    val timestamp: Long = System.currentTimeMillis(),
    val category: String // "STOCKS", "FUNDS", "RETIREMENT"
)
