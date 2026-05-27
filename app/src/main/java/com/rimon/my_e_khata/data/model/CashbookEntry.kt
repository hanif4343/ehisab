package com.rimon.my_e_khata.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cashbook")
data class CashbookEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,       // "in" or "out"
    val amount: Double,
    val balance: Double,
    val category: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
