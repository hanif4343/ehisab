package com.rimon.my_e_khata.ui.cashbook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rimon.my_e_khata.data.db.AppDatabase
import com.rimon.my_e_khata.data.model.CashbookEntry
import kotlinx.coroutines.launch

class CashbookViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).cashbookDao()

    val entries = dao.getAllEntries()
    val currentBalance = MutableLiveData(0.0)
    val totalIn = MutableLiveData(0.0)
    val totalOut = MutableLiveData(0.0)

    init { refreshTotals() }

    fun refreshTotals() {
        viewModelScope.launch {
            currentBalance.value = dao.getCurrentBalance() ?: 0.0
            totalIn.value = dao.getTotalCashIn() ?: 0.0
            totalOut.value = dao.getTotalCashOut() ?: 0.0
        }
    }

    fun addEntry(type: String, amount: Double, note: String = "", category: String = "") {
        viewModelScope.launch {
            val currentBal = dao.getCurrentBalance() ?: 0.0
            val newBalance = if (type == "in") currentBal + amount else currentBal - amount
            dao.insertEntry(CashbookEntry(type = type, amount = amount, balance = newBalance, note = note, category = category))
            refreshTotals()
        }
    }

    fun deleteEntry(entry: CashbookEntry) {
        viewModelScope.launch {
            dao.deleteEntry(entry)
            refreshTotals()
        }
    }
}
