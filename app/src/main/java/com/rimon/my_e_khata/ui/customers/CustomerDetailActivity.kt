package com.rimon.my_e_khata.ui.customers

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.rimon.my_e_khata.R
import com.rimon.my_e_khata.data.db.AppDatabase
import com.rimon.my_e_khata.data.model.Customer
import com.rimon.my_e_khata.databinding.ActivityCustomerDetailBinding
import com.rimon.my_e_khata.service.SmsService
import com.rimon.my_e_khata.ui.common.CalculatorDialog
import com.rimon.my_e_khata.ui.common.TransactionAdapter
import com.rimon.my_e_khata.utils.AppPreferences
import com.rimon.my_e_khata.utils.FormatUtils
import com.rimon.my_e_khata.utils.PdfGenerator
import kotlinx.coroutines.launch
import kotlin.math.abs

class CustomerDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerDetailBinding
    private lateinit var viewModel: CustomerViewModel
    private var customerId: Long = -1
    private var customer: Customer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        customerId = intent.getLongExtra("customer_id", -1)
        if (customerId == -1L) { finish(); return }

        viewModel = ViewModelProvider(this)[CustomerViewModel::class.java]

        setupTransactionList()
        observeData()
        setupClickListeners()
    }

    private fun setupTransactionList() {
        val adapter = TransactionAdapter(
            currencySymbol = "৳",
            onDelete = { tx ->
                AlertDialog.Builder(this)
                    .setTitle("Delete Entry")
                    .setMessage("Are you sure you want to delete this entry?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteTransaction(tx, customerId)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        binding.recyclerTransactions.adapter = adapter

        viewModel.getTransactions(customerId).observe(this) { transactions ->
            adapter.submitList(transactions)
        }
    }

    private fun observeData() {
        viewModel.customers.observe(this) { customers ->
            customer = customers.find { it.id == customerId }
            customer?.let { updateUI(it) }
        }
    }

    private fun updateUI(customer: Customer) {
        binding.toolbar.title = customer.name
        val balance = customer.balance
        if (balance >= 0) {
            binding.tvBalance.text = FormatUtils.formatAmount(balance)
            binding.tvBalanceLabel.text = "You will get"
            binding.tvBalance.setTextColor(getColor(R.color.red_due))
        } else {
            binding.tvBalance.text = FormatUtils.formatAmount(abs(balance))
            binding.tvBalanceLabel.text = "You will give"
            binding.tvBalance.setTextColor(getColor(R.color.green_settled))
        }
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnReport.setOnClickListener {
            lifecycleScope.launch {
                val customer = customer ?: return@launch
                val transactions = AppDatabase.getDatabase(this@CustomerDetailActivity)
                    .transactionDao().getTransactionsForPartySync(customerId, "customer")
                val file = PdfGenerator.generateCustomerReport(this@CustomerDetailActivity, customer, transactions)
                PdfGenerator.shareFile(this@CustomerDetailActivity, file)
            }
        }

        binding.btnSendSmsNow.setOnClickListener {
            val customer = customer ?: return@setOnClickListener
            if (customer.mobile.isBlank()) {
                Toast.makeText(this, "No mobile number set", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                val prefs = AppPreferences.getInstance(this@CustomerDetailActivity)
                val message = SmsService.buildMessage(
                    prefs.smsTemplateCustomer,
                    customer.name,
                    FormatUtils.formatAmount(customer.balance, ""),
                    prefs.businessName
                )
                val result = SmsService.sendSms(this@CustomerDetailActivity, customer.mobile, message)
                runOnUiThread {
                    if (result.isSuccess) {
                        Toast.makeText(this@CustomerDetailActivity, "SMS sent!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@CustomerDetailActivity, "Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        binding.btnToggleAutoSms.setOnClickListener {
            customer?.let { viewModel.toggleAutoSms(it) }
        }

        binding.btnYouGave.setOnClickListener {
            showAddTransactionDialog("gave")
        }

        binding.btnYouGot.setOnClickListener {
            showAddTransactionDialog("got")
        }

        binding.btnSetReminder.setOnClickListener {
            showReminderDialog()
        }
    }

    private fun showAddTransactionDialog(type: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_transaction, null)
        val etAmount = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_amount)
        val etNote = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_note)
        val btnCalc = dialogView.findViewById<android.widget.ImageButton>(R.id.btn_calculator)

        btnCalc.setOnClickListener {
            CalculatorDialog(this) { result ->
                etAmount.setText(result)
            }.show()
        }

        AlertDialog.Builder(this)
            .setTitle(if (type == "gave") "You Gave" else "You Got")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val amount = FormatUtils.parseAmount(etAmount.text?.toString() ?: "")
                val note = etNote.text?.toString()?.trim() ?: ""
                if (amount > 0) {
                    viewModel.addTransaction(customerId, type, amount, note)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showReminderDialog() {
        // Date picker for collection reminder
        val cal = java.util.Calendar.getInstance()
        android.app.DatePickerDialog(
            this,
            { _, year, month, day ->
                Toast.makeText(this, "Reminder set for $day/${month+1}/$year", Toast.LENGTH_SHORT).show()
            },
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH),
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }
}
