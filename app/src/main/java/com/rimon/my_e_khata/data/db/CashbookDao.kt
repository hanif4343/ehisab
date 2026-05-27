package com.rimon.my_e_khata.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.rimon.my_e_khata.data.model.CashbookEntry

@Dao
interface CashbookDao {
    @Query("SELECT * FROM cashbook ORDER BY createdAt DESC")
    fun getAllEntries(): LiveData<List<CashbookEntry>>

    @Query("SELECT * FROM cashbook ORDER BY createdAt DESC")
    suspend fun getAllEntriesSync(): List<CashbookEntry>

    @Query("SELECT * FROM cashbook WHERE strftime('%Y-%m', datetime(createdAt/1000,'unixepoch')) = :yearMonth ORDER BY createdAt DESC")
    fun getEntriesByMonth(yearMonth: String): LiveData<List<CashbookEntry>>

    @Insert
    suspend fun insertEntry(entry: CashbookEntry): Long

    @Delete
    suspend fun deleteEntry(entry: CashbookEntry)

    @Query("SELECT SUM(CASE WHEN type='in' THEN amount ELSE 0 END) - SUM(CASE WHEN type='out' THEN amount ELSE 0 END) FROM cashbook")
    suspend fun getCurrentBalance(): Double?

    @Query("SELECT SUM(amount) FROM cashbook WHERE type='in'")
    suspend fun getTotalCashIn(): Double?

    @Query("SELECT SUM(amount) FROM cashbook WHERE type='out'")
    suspend fun getTotalCashOut(): Double?
}
