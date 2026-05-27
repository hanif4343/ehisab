package com.rimon.my_e_khata.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.rimon.my_e_khata.data.model.SmsLog

@Dao
interface SmsLogDao {
    @Query("SELECT * FROM sms_logs ORDER BY sentAt DESC")
    fun getAllLogs(): LiveData<List<SmsLog>>

    @Insert
    suspend fun insert(log: SmsLog)

    @Query("DELETE FROM sms_logs")
    suspend fun clearAll()
}
