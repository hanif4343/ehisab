package com.rimon.my_e_khata.ui.customers

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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

        // Setup RecyclerView
        val adapter = TransactionAdapter(
            currencySymbol = "৳",
            onDelete = { tx ->
                AlertDialog.Builder(this)
                    .setTitle("Delete Entry")
                    .setMessage("Are you sure?")
                    .setPositiveButton("Delete") { _, _ -> viewModel.deleteTransaction(tx, customerId) }
                    .setNegativeButton("Cancel", null).show()
            }
        )
        binding.recyclerTransactions.layoutManager = LinearLayoutManager(this)
        binding.recyclerTransactions.adapter = adapter

        viewModel.getTransactions(customerId).observe(this) { txList ->
            adapter.submitList(txList)
        }

        // Observe customer data
        viewModel.customers.observe(this) { list ->
            customer = list.find { it.id == customerId }
            customer?.let { updateUI(it) }
        }

        setupClickListeners()
    }

    private fun updateUI(c: Customer) {
        binding.toolbar.title = c.name
        val balance = c.balance
        when {
            balance > 0 -> {
                binding.tvBalanceLabel.text = "You will get"
                binding.tvBalance.text = FormatUtils.formatAmount(balance)
                binding.tvBalance.setTextColor(getColor(R.color.red_due))
            }
            balance < 0 -> {
                binding.tvBalanceLabel.text = "You will give"
                binding.tvBalance.text = FormatUtils.formatAmount(abs(balance))
                binding.tvBalance.setTextColor(getColor(R.color.green_settled))
            }
            else -> {
                binding.tvBalanceLabel.text = "Settled Up"
                binding.tvBalance.text = FormatUtils.formatAmount(0.0)
                binding.tvBalance.setTextColor(getColor(R.color.green_settled))
            }
        }
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnReport.setOnClickListener {
            lifecycleScope.launch {
                val c = customer ?: return@launch
                val txs = AppDatabase.getDatabase(this@CustomerDetailActivity)
                    .transactionDao().getTransactionsForPartySync(customerId, "customer")
                val file = PdfGenerator.generateCustomerReport(this@CustomerDetailActivity, c, txs)
                PdfGenerator.shareFile(this@CustomerDetailActivity, file)
            }
        }

        binding.btnReminder.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(this, { _, y, m, d ->
                Toast.makeText(this, "Reminder set: $d/${m+1}/$y", Toast.LENGTH_SHORT).show()
            }, cal.get(java.util.Calendar.YEAR),
               cal.get(java.util.Calendar.MONTH),
               cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnSetReminder.setOnClickListener {
            binding.btnReminder.performClick()
        }

        binding.btnSendSmsNow.setOnClickListener {
            val c = customer ?: return@setOnClickListener
            if (c.mobile.isBlank()) {
                Toast.makeText(this, "No mobile number saved for this customer", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                val prefs = AppPreferences.getInstance(this@CustomerDetailActivity)
                val msg = SmsService.buildMessage(
                    prefs.smsTemplateCustomer, c.name,
                    FormatUtils.formatAmount(c.balance, ""), prefs.businessName
                )
                val result = SmsService.sendSms(this@CustomerDetailActivity, c.name, c.mobile, msg)
                runOnUiThread {
                    if (result.isSuccess)
                        Toast.makeText(this@CustomerDetailActivity, "SMS sent to ${c.mobile}!", Toast.LENGTH_SHORT).show()
                    else
                        Toast.makeText(this@CustomerDetailActivity, "SMS failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.btnYouGave.setOnClickListener { showTransactionDialog("gave") }
        binding.btnYouGot.setOnClickListener  { showTransactionDialog("got") }
    }

    private fun showTransactionDialog(type: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_transaction, null)
        val etAmount = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_amount)
        val etNote   = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_note)

        dialogView.findViewById<android.widget.ImageButton>(R.id.btn_calculator)?.setOnClickListener {
            CalculatorDialog(this) { result -> etAmount.setText(result) }.show()
        }

        AlertDialog.Builder(this)
            .setTitle(if (type == "gave") "You Gave ৳" else "You Got ৳")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val amount = FormatUtils.parseAmount(etAmount.text?.toString() ?: "")
                val note   = etNote.text?.toString()?.trim() ?: ""
                if (amount > 0) {
                    viewModel.addTransaction(customerId, type, amount, note)
                } else {
                    Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null).show()
    }
}
