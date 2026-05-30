package com.rimon.my_e_khata.ui.suppliers

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.rimon.my_e_khata.R
import com.rimon.my_e_khata.data.db.AppDatabase
import com.rimon.my_e_khata.data.model.Supplier
import com.rimon.my_e_khata.databinding.ActivityCustomerDetailBinding
import com.rimon.my_e_khata.service.SmsService
import com.rimon.my_e_khata.ui.common.CalculatorDialog
import com.rimon.my_e_khata.ui.common.TransactionAdapter
import com.rimon.my_e_khata.utils.AppPreferences
import com.rimon.my_e_khata.utils.FormatUtils
import com.rimon.my_e_khata.utils.PdfGenerator
import kotlinx.coroutines.launch
import kotlin.math.abs

class SupplierDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerDetailBinding
    private lateinit var viewModel: SupplierViewModel
    private var supplierId: Long = -1
    private var supplier: Supplier? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supplierId = intent.getLongExtra("supplier_id", -1)
        if (supplierId == -1L) { finish(); return }

        viewModel = ViewModelProvider(this)[SupplierViewModel::class.java]

        val adapter = TransactionAdapter(onDelete = { tx ->
            AlertDialog.Builder(this).setTitle("Delete Entry")
                .setPositiveButton("Delete") { _, _ -> viewModel.deleteTransaction(tx, supplierId) }
                .setNegativeButton("Cancel", null).show()
        })
        binding.recyclerTransactions.layoutManager = LinearLayoutManager(this)
        binding.recyclerTransactions.adapter = adapter
        viewModel.getTransactions(supplierId).observe(this) { adapter.submitList(it) }

        viewModel.suppliers.observe(this) { list ->
            supplier = list.find { it.id == supplierId }
            supplier?.let {
                binding.toolbar.title = it.name
                val b = it.balance
                when {
                    b > 0 -> {
                        binding.tvBalanceLabel.text = "You will give"
                        binding.tvBalance.text = FormatUtils.formatAmount(b)
                        binding.tvBalance.setTextColor(getColor(R.color.red_due))
                    }
                    b < 0 -> {
                        binding.tvBalanceLabel.text = "You will get"
                        binding.tvBalance.text = FormatUtils.formatAmount(abs(b))
                        binding.tvBalance.setTextColor(getColor(R.color.green_settled))
                    }
                    else -> {
                        binding.tvBalanceLabel.text = "Settled Up"
                        binding.tvBalance.text = FormatUtils.formatAmount(0.0)
                        binding.tvBalance.setTextColor(getColor(R.color.green_settled))
                    }
                }
            }
        }

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnReport.setOnClickListener {
            lifecycleScope.launch {
                val s = supplier ?: return@launch
                val txs = AppDatabase.getDatabase(this@SupplierDetailActivity)
                    .transactionDao().getTransactionsForPartySync(supplierId, "supplier")
                val file = PdfGenerator.generateSupplierReport(this@SupplierDetailActivity, s, txs)
                PdfGenerator.shareFile(this@SupplierDetailActivity, file)
            }
        }

        binding.btnSendSmsNow.setOnClickListener {
            val s = supplier ?: return@setOnClickListener
            if (s.mobile.isBlank()) {
                Toast.makeText(this, "No mobile number saved", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                val prefs = AppPreferences.getInstance(this@SupplierDetailActivity)
                val msg = SmsService.buildMessage(
                    prefs.smsTemplateCustomer, s.name,
                    FormatUtils.formatAmount(s.balance, ""), prefs.businessName
                )
                val result = SmsService.sendSms(this@SupplierDetailActivity, s.name, s.mobile, msg)
                runOnUiThread {
                    Toast.makeText(this@SupplierDetailActivity,
                        if (result.isSuccess) "SMS sent!" else "Failed: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT).show()
                }
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

        binding.btnSetReminder.setOnClickListener { binding.btnReminder.performClick() }

        binding.btnYouGave.setOnClickListener { showDialog("gave") }
        binding.btnYouGot.setOnClickListener  { showDialog("got") }
    }

    private fun showDialog(type: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_transaction, null)
        val etAmount = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_amount)
        val etNote   = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_note)
        dialogView.findViewById<android.widget.ImageButton>(R.id.btn_calculator)?.setOnClickListener {
            CalculatorDialog(this) { etAmount.setText(it) }.show()
        }
        AlertDialog.Builder(this)
            .setTitle(if (type == "gave") "You Gave ৳" else "You Got ৳")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val amount = FormatUtils.parseAmount(etAmount.text?.toString() ?: "")
                if (amount > 0) viewModel.addTransaction(supplierId, type, amount, etNote.text?.toString()?.trim() ?: "")
                else Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null).show()
    }
}
