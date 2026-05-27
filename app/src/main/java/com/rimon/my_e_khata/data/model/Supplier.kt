package com.rimon.my_e_khata.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val mobile: String = "",
    val email: String = "",
    val address: String = "",
    val governmentId: String = "",
    val balance: Double = 0.0, // positive = you will give, negative = you will get
    val autoSmsEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
