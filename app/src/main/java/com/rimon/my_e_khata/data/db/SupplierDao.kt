package com.rimon.my_e_khata.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.rimon.my_e_khata.data.model.Supplier

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY updatedAt DESC")
    fun getAllSuppliers(): LiveData<List<Supplier>>

    @Query("SELECT * FROM suppliers WHERE name LIKE '%' || :query || '%' OR mobile LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchSuppliers(query: String): LiveData<List<Supplier>>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun getSupplierById(id: Long): Supplier?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier): Long

    @Update
    suspend fun updateSupplier(supplier: Supplier)

    @Delete
    suspend fun deleteSupplier(supplier: Supplier)

    @Query("SELECT SUM(CASE WHEN balance > 0 THEN balance ELSE 0 END) FROM suppliers")
    suspend fun getTotalPayable(): Double?

    @Query("SELECT SUM(CASE WHEN balance < 0 THEN ABS(balance) ELSE 0 END) FROM suppliers")
    suspend fun getTotalReceivable(): Double?

    @Query("SELECT * FROM suppliers WHERE autoSmsEnabled = 1 AND mobile != '' AND balance > 0")
    suspend fun getSuppliersForAutoSms(): List<Supplier>
}
