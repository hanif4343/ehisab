package com.rimon.my_e_khata.ui.suppliers

import android.app.Application
import androidx.lifecycle.*
import com.rimon.my_e_khata.data.db.AppDatabase
import com.rimon.my_e_khata.data.model.Supplier
import com.rimon.my_e_khata.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SupplierViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val supplierDao = db.supplierDao()
    private val transactionDao = db.transactionDao()

    private val _searchQuery = MutableLiveData("")

    val suppliers: LiveData<List<Supplier>> = _searchQuery.switchMap { query ->
        if (query.isBlank()) supplierDao.getAllSuppliers()
        else supplierDao.searchSuppliers(query)
    }

    private val _totalPayable    = MutableLiveData(0.0)
    val totalPayable: LiveData<Double> = _totalPayable

    private val _totalReceivable = MutableLiveData(0.0)
    val totalReceivable: LiveData<Double> = _totalReceivable

    private val _navigateBack = MutableLiveData(false)
    val navigateBack: LiveData<Boolean> = _navigateBack

    init { refreshTotals() }

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun refreshTotals() {
        viewModelScope.launch {
            val payable    = withContext(Dispatchers.IO) { supplierDao.getTotalPayable()    ?: 0.0 }
            val receivable = withContext(Dispatchers.IO) { supplierDao.getTotalReceivable() ?: 0.0 }
            _totalPayable.value    = payable
            _totalReceivable.value = receivable
        }
    }

    fun addSupplier(supplier: Supplier) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { supplierDao.insertSupplier(supplier) }
            refreshTotals()
            _navigateBack.value = true
        }
    }

    fun resetNavigateBack() { _navigateBack.value = false }

    fun deleteSupplier(supplier: Supplier) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                transactionDao.deleteAllTransactionsForParty(supplier.id, "supplier")
                supplierDao.deleteSupplier(supplier)
            }
            refreshTotals()
        }
    }

    fun addTransaction(supplierId: Long, type: String, amount: Double, note: String = "") {
        viewModelScope.launch {
            val supplier = withContext(Dispatchers.IO) { supplierDao.getSupplierById(supplierId) } ?: return@launch
            val newBalance = when (type) {
                "gave" -> supplier.balance + amount
                "got"  -> supplier.balance - amount
                else   -> supplier.balance
            }
            withContext(Dispatchers.IO) {
                transactionDao.insertTransaction(
                    Transaction(
                        partyId = supplierId, partyType = "supplier",
                        type = type, amount = amount,
                        balance = newBalance, note = note
                    )
                )
                supplierDao.updateSupplier(
                    supplier.copy(balance = newBalance, updatedAt = System.currentTimeMillis())
                )
            }
            refreshTotals()
        }
    }

    fun deleteTransaction(transaction: Transaction, supplierId: Long) {
        viewModelScope.launch {
            val supplier = withContext(Dispatchers.IO) { supplierDao.getSupplierById(supplierId) } ?: return@launch
            val revertedBalance = when (transaction.type) {
                "gave" -> supplier.balance - transaction.amount
                "got"  -> supplier.balance + transaction.amount
                else   -> supplier.balance
            }
            withContext(Dispatchers.IO) {
                transactionDao.deleteTransaction(transaction)
                supplierDao.updateSupplier(
                    supplier.copy(balance = revertedBalance, updatedAt = System.currentTimeMillis())
                )
            }
            refreshTotals()
        }
    }

    fun getTransactions(supplierId: Long): LiveData<List<Transaction>> =
        transactionDao.getTransactionsForParty(supplierId, "supplier")

    fun toggleAutoSms(supplier: Supplier) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                supplierDao.updateSupplier(supplier.copy(autoSmsEnabled = !supplier.autoSmsEnabled))
            }
        }
    }
}
