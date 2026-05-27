package com.rimon.my_e_khata.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.rimon.my_e_khata.data.model.Transaction

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE partyId = :partyId AND partyType = :partyType ORDER BY createdAt DESC")
    fun getTransactionsForParty(partyId: Long, partyType: String): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE partyId = :partyId AND partyType = :partyType ORDER BY createdAt DESC")
    suspend fun getTransactionsForPartySync(partyId: Long, partyType: String): List<Transaction>

    @Insert
    suspend fun insertTransaction(transaction: Transaction): Long

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE partyId = :partyId AND partyType = :partyType")
    suspend fun deleteAllTransactionsForParty(partyId: Long, partyType: String)
}
