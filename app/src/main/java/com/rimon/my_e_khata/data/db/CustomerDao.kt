package com.rimon.my_e_khata.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.rimon.my_e_khata.data.model.Customer

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY updatedAt DESC")
    fun getAllCustomers(): LiveData<List<Customer>>

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR mobile LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchCustomers(query: String): LiveData<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("SELECT SUM(CASE WHEN balance > 0 THEN balance ELSE 0 END) FROM customers")
    suspend fun getTotalReceivable(): Double?

    @Query("SELECT SUM(CASE WHEN balance < 0 THEN ABS(balance) ELSE 0 END) FROM customers")
    suspend fun getTotalPayable(): Double?

    @Query("SELECT * FROM customers WHERE autoSmsEnabled = 1 AND mobile != '' AND balance > 0")
    suspend fun getCustomersForAutoSms(): List<Customer>

    @Query("SELECT COUNT(*) FROM transactions WHERE partyId = :customerId AND partyType = 'customer'")
    suspend fun getTransactionCount(customerId: Long): Int
}
