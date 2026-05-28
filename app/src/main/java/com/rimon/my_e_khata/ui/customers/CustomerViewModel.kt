package com.rimon.my_e_khata.ui.customers

import android.app.Application
import androidx.lifecycle.*
import com.rimon.my_e_khata.data.db.AppDatabase
import com.rimon.my_e_khata.data.model.Customer
import com.rimon.my_e_khata.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val customerDao = db.customerDao()
    private val transactionDao = db.transactionDao()

    private val _searchQuery = MutableLiveData("")

    val customers: LiveData<List<Customer>> = _searchQuery.switchMap { query ->
        if (query.isBlank()) customerDao.getAllCustomers()
        else customerDao.searchCustomers(query)
    }

    private val _totalReceivable = MutableLiveData(0.0)
    val totalReceivable: LiveData<Double> = _totalReceivable

    private val _totalPayable = MutableLiveData(0.0)
    val totalPayable: LiveData<Double> = _totalPayable

    private val _navigateBack = MutableLiveData(false)
    val navigateBack: LiveData<Boolean> = _navigateBack

    init { refreshTotals() }

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun refreshTotals() {
        viewModelScope.launch(Dispatchers.IO) {
            val receivable = customerDao.getTotalReceivable() ?: 0.0
            val payable    = customerDao.getTotalPayable()    ?: 0.0
            _totalReceivable.postValue(receivable)
            _totalPayable.postValue(payable)
        }
    }

    fun addCustomer(customer: Customer) {
        viewModelScope.launch(Dispatchers.IO) {
            customerDao.insertCustomer(customer)
            val receivable = customerDao.getTotalReceivable() ?: 0.0
            val payable    = customerDao.getTotalPayable()    ?: 0.0
            _totalReceivable.postValue(receivable)
            _totalPayable.postValue(payable)
            _navigateBack.postValue(true)   // postValue = safe from any thread
        }
    }

    fun resetNavigateBack() { _navigateBack.value = false }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch(Dispatchers.IO) {
            customerDao.updateCustomer(customer)
            refreshTotals()
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionDao.deleteAllTransactionsForParty(customer.id, "customer")
            customerDao.deleteCustomer(customer)
            refreshTotals()
        }
    }

    fun addTransaction(customerId: Long, type: String, amount: Double, note: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val customer = customerDao.getCustomerById(customerId) ?: return@launch
            val newBalance = when (type) {
                "got"  -> customer.balance + amount
                "gave" -> customer.balance - amount
                else   -> customer.balance
            }
            transactionDao.insertTransaction(
                Transaction(
                    partyId = customerId, partyType = "customer",
                    type = type, amount = amount,
                    balance = newBalance, note = note
                )
            )
            customerDao.updateCustomer(
                customer.copy(balance = newBalance, updatedAt = System.currentTimeMillis())
            )
            refreshTotals()
        }
    }

    fun deleteTransaction(transaction: Transaction, customerId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val customer = customerDao.getCustomerById(customerId) ?: return@launch
            val revertedBalance = when (transaction.type) {
                "got"  -> customer.balance - transaction.amount
                "gave" -> customer.balance + transaction.amount
                else   -> customer.balance
            }
            transactionDao.deleteTransaction(transaction)
            customerDao.updateCustomer(
                customer.copy(balance = revertedBalance, updatedAt = System.currentTimeMillis())
            )
            refreshTotals()
        }
    }

    fun getTransactions(customerId: Long): LiveData<List<Transaction>> =
        transactionDao.getTransactionsForParty(customerId, "customer")

    fun toggleAutoSms(customer: Customer) {
        viewModelScope.launch(Dispatchers.IO) {
            customerDao.updateCustomer(customer.copy(autoSmsEnabled = !customer.autoSmsEnabled))
        }
    }
}
