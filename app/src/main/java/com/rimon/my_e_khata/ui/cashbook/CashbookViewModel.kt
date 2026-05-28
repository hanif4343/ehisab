package com.rimon.my_e_khata.ui.cashbook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rimon.my_e_khata.data.db.AppDatabase
import com.rimon.my_e_khata.data.model.CashbookEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CashbookViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).cashbookDao()

    val entries = dao.getAllEntries()
    val currentBalance = MutableLiveData(0.0)
    val totalIn        = MutableLiveData(0.0)
    val totalOut       = MutableLiveData(0.0)

    init { refreshTotals() }

    fun refreshTotals() {
        viewModelScope.launch(Dispatchers.IO) {
            currentBalance.postValue(dao.getCurrentBalance() ?: 0.0)
            totalIn.postValue(dao.getTotalCashIn()  ?: 0.0)
            totalOut.postValue(dao.getTotalCashOut() ?: 0.0)
        }
    }

    fun addEntry(type: String, amount: Double, note: String = "", category: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val currentBal = dao.getCurrentBalance() ?: 0.0
            val newBalance = if (type == "in") currentBal + amount else currentBal - amount
            dao.insertEntry(CashbookEntry(
                type = type, amount = amount,
                balance = newBalance, note = note, category = category
            ))
            currentBalance.postValue(dao.getCurrentBalance() ?: 0.0)
            totalIn.postValue(dao.getTotalCashIn()  ?: 0.0)
            totalOut.postValue(dao.getTotalCashOut() ?: 0.0)
        }
    }

    fun deleteEntry(entry: CashbookEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteEntry(entry)
            currentBalance.postValue(dao.getCurrentBalance() ?: 0.0)
            totalIn.postValue(dao.getTotalCashIn()  ?: 0.0)
            totalOut.postValue(dao.getTotalCashOut() ?: 0.0)
        }
    }
}
