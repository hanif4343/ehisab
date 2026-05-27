package com.rimon.my_e_khata.ui.customers

import android.app.Application
import androidx.lifecycle.*
import com.rimon.my_e_khata.data.db.AppDatabase
import com.rimon.my_e_khata.data.model.Customer
import com.rimon.my_e_khata.data.model.Transaction
import kotlinx.coroutines.launch

class CustomerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val customerDao = db.customerDao()
    private val transactionDao = db.transactionDao()

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    val customers: LiveData<List<Customer>> = _searchQuery.switchMap { query ->
        if (query.isBlank()) customerDao.getAllCustomers()
        else customerDao.searchCustomers(query)
    }

    private val _totalReceivable = MutableLiveData(0.0)
    val totalReceivable: LiveData<Double> = _totalReceivable

    private val _totalPayable = MutableLiveData(0.0)
    val totalPayable: LiveData<Double> = _totalPayable

    init {
        refreshTotals()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refreshTotals() {
        viewModelScope.launch {
            _totalReceivable.value = customerDao.getTotalReceivable() ?: 0.0
            _totalPayable.value = customerDao.getTotalPayable() ?: 0.0
        }
    }

    fun addCustomer(customer: Customer, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = customerDao.insertCustomer(customer)
            refreshTotals()
            onDone(id)
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            customerDao.updateCustomer(customer)
            refreshTotals()
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            transactionDao.deleteAllTransactionsForParty(customer.id, "customer")
            customerDao.deleteCustomer(customer)
            refreshTotals()
        }
    }

    fun addTransaction(
        customerId: Long,
        type: String,
        amount: Double,
        note: String = "",
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val customer = customerDao.getCustomerById(customerId) ?: return@launch
            val newBalance = when (type) {
                "got" -> customer.balance + amount   // customer paid us
                "gave" -> customer.balance - amount  // we gave credit
                else -> customer.balance
            }
            val tx = Transaction(
                partyId = customerId,
                partyType = "customer",
                type = type,
                amount = amount,
                balance = newBalance,
                note = note
            )
            transactionDao.insertTransaction(tx)
            customerDao.updateCustomer(
                customer.copy(balance = newBalance, updatedAt = System.currentTimeMillis())
            )
            refreshTotals()
            onDone()
        }
    }

    fun deleteTransaction(transaction: Transaction, customerId: Long) {
        viewModelScope.launch {
            val customer = customerDao.getCustomerById(customerId) ?: return@launch
            // Reverse the transaction
            val revertedBalance = when (transaction.type) {
                "got" -> customer.balance - transaction.amount
                "gave" -> customer.balance + transaction.amount
                else -> customer.balance
            }
            transactionDao.deleteTransaction(transaction)
            customerDao.updateCustomer(
                customer.copy(balance = revertedBalance, updatedAt = System.currentTimeMillis())
            )
            refreshTotals()
        }
    }

    fun getTransactions(customerId: Long): LiveData<List<Transaction>> {
        return transactionDao.getTransactionsForParty(customerId, "customer")
    }

    fun toggleAutoSms(customer: Customer) {
        viewModelScope.launch {
            customerDao.updateCustomer(customer.copy(autoSmsEnabled = !customer.autoSmsEnabled))
        }
    }
}
