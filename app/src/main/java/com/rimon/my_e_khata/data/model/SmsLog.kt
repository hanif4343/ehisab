package com.rimon.my_e_khata.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_logs")
data class SmsLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recipientName: String,
    val recipientNumber: String,
    val message: String,
    val status: String = "sent", // "sent" or "failed"
    val sentAt: Long = System.currentTimeMillis()
)
