package com.rimon.my_e_khata.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val partyId: Long,          // customer or supplier id
    val partyType: String,       // "customer" or "supplier"
    val type: String,            // "gave" or "got"
    val amount: Double,
    val balance: Double,         // running balance after this transaction
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
