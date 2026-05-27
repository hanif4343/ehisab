package com.rimon.my_e_khata.ui.suppliers

import android.app.Application
import androidx.lifecycle.*
import com.rimon.my_e_khata.data.db.AppDatabase
import com.rimon.my_e_khata.data.model.Supplier
import com.rimon.my_e_khata.data.model.Transaction
import kotlinx.coroutines.launch

class SupplierViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val supplierDao = db.supplierDao()
    private val transactionDao = db.transactionDao()

    private val _searchQuery = MutableLiveData("")

    val suppliers: LiveData<List<Supplier>> = _searchQuery.switchMap { query ->
        if (query.isBlank()) supplierDao.getAllSuppliers()
        else supplierDao.searchSuppliers(query)
    }

    private val _totalPayable = MutableLiveData(0.0)
    val totalPayable: LiveData<Double> = _totalPayable

    private val _totalReceivable = MutableLiveData(0.0)
    val totalReceivable: LiveData<Double> = _totalReceivable

    init { refreshTotals() }

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun refreshTotals() {
        viewModelScope.launch {
            _totalPayable.value = supplierDao.getTotalPayable() ?: 0.0
            _totalReceivable.value = supplierDao.getTotalReceivable() ?: 0.0
        }
    }

    fun addSupplier(supplier: Supplier, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = supplierDao.insertSupplier(supplier)
            refreshTotals()
            onDone(id)
        }
    }

    fun deleteSupplier(supplier: Supplier) {
        viewModelScope.launch {
            transactionDao.deleteAllTransactionsForParty(supplier.id, "supplier")
            supplierDao.deleteSupplier(supplier)
            refreshTotals()
        }
    }

    fun addTransaction(supplierId: Long, type: String, amount: Double, note: String = "", onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val supplier = supplierDao.getSupplierById(supplierId) ?: return@launch
            val newBalance = when (type) {
                "gave" -> supplier.balance + amount  // we paid supplier
                "got" -> supplier.balance - amount   // supplier gave us credit
                else -> supplier.balance
            }
            transactionDao.insertTransaction(
                Transaction(partyId = supplierId, partyType = "supplier", type = type, amount = amount, balance = newBalance, note = note)
            )
            supplierDao.updateSupplier(supplier.copy(balance = newBalance, updatedAt = System.currentTimeMillis()))
            refreshTotals()
            onDone()
        }
    }

    fun deleteTransaction(transaction: Transaction, supplierId: Long) {
        viewModelScope.launch {
            val supplier = supplierDao.getSupplierById(supplierId) ?: return@launch
            val revertedBalance = when (transaction.type) {
                "gave" -> supplier.balance - transaction.amount
                "got" -> supplier.balance + transaction.amount
                else -> supplier.balance
            }
            transactionDao.deleteTransaction(transaction)
            supplierDao.updateSupplier(supplier.copy(balance = revertedBalance, updatedAt = System.currentTimeMillis()))
            refreshTotals()
        }
    }

    fun getTransactions(supplierId: Long): LiveData<List<Transaction>> =
        transactionDao.getTransactionsForParty(supplierId, "supplier")

    fun toggleAutoSms(supplier: Supplier) {
        viewModelScope.launch {
            supplierDao.updateSupplier(supplier.copy(autoSmsEnabled = !supplier.autoSmsEnabled))
        }
    }
}
