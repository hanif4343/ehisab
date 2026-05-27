package com.rimon.my_e_khata.ui.settings

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.rimon.my_e_khata.data.db.AppDatabase
import com.rimon.my_e_khata.databinding.ActivitySettingsBinding
import com.rimon.my_e_khata.service.AutoSmsWorker
import com.rimon.my_e_khata.utils.AppPreferences
import com.rimon.my_e_khata.utils.BackupManager
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: AppPreferences
    private lateinit var smsLogAdapter: SmsLogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = AppPreferences.getInstance(this)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupSmsLogRecycler()
        loadSettings()
        setupListeners()
    }

    private fun setupSmsLogRecycler() {
        smsLogAdapter = SmsLogAdapter()
        binding.recyclerSmsLog.layoutManager = LinearLayoutManager(this)
        binding.recyclerSmsLog.adapter = smsLogAdapter
        binding.recyclerSmsLog.isNestedScrollingEnabled = false

        AppDatabase.getDatabase(this).smsLogDao().getAllLogs().observe(this) { logs ->
            smsLogAdapter.submitList(logs)
            binding.tvSmsLogEmpty.visibility =
                if (logs.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            binding.tvSmsLogCount.text = "${logs.size} messages sent"
        }
    }

    private fun loadSettings() {
        binding.etBusinessName.setText(prefs.businessName)
        binding.etSmsApiUrl.setText(prefs.smsApiUrl)
        binding.etSmsApiKey.setText(prefs.smsApiKey)
        binding.etSmsSenderId.setText(prefs.smsSenderId)
        binding.etSmsTemplateCustomer.setText(prefs.smsTemplateCustomer)
        binding.switchAutoSms.isChecked = prefs.autoSmsEnabledGlobal
        updateSmsTimeDisplay()
        binding.etGmailEmail.setText(prefs.gmailBackupEmail)
        binding.switchAutoBackup.isChecked = prefs.autoBackupEnabled
        binding.etCurrencySymbol.setText(prefs.currencySymbol)

        // Set all fields non-editable initially
        setFieldsEditable(false)
    }

    private fun setFieldsEditable(editable: Boolean) {
        listOf(
            binding.etBusinessName, binding.etSmsApiUrl, binding.etSmsApiKey,
            binding.etSmsSenderId, binding.etSmsTemplateCustomer,
            binding.etGmailEmail, binding.etCurrencySymbol
        ).forEach { it.isEnabled = editable }

        binding.btnEditSettings.text = if (editable) "✕ Cancel" else "✎ Edit"
        binding.btnSaveAll.visibility = if (editable) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun setupListeners() {

        // Edit / Cancel toggle
        binding.btnEditSettings.setOnClickListener {
            val isEditing = binding.btnSaveAll.visibility == android.view.View.VISIBLE
            if (isEditing) {
                // Cancel — reload original values
                loadSettings()
            } else {
                setFieldsEditable(true)
            }
        }

        // Save all
        binding.btnSaveAll.setOnClickListener {
            val businessName = binding.etBusinessName.text?.toString()?.trim() ?: ""
            if (businessName.isBlank()) {
                binding.etBusinessName.error = "Business name required"
                return@setOnClickListener
            }
            prefs.businessName      = businessName
            prefs.currencySymbol    = binding.etCurrencySymbol.text?.toString()?.trim()?.ifBlank { "৳" } ?: "৳"
            prefs.smsApiUrl         = binding.etSmsApiUrl.text?.toString()?.trim() ?: ""
            prefs.smsApiKey         = binding.etSmsApiKey.text?.toString()?.trim() ?: ""
            prefs.smsSenderId       = binding.etSmsSenderId.text?.toString()?.trim() ?: ""
            prefs.smsTemplateCustomer = binding.etSmsTemplateCustomer.text?.toString()?.trim()
                ?: prefs.smsTemplateCustomer
            prefs.gmailBackupEmail  = binding.etGmailEmail.text?.toString()?.trim() ?: ""
            prefs.autoBackupEnabled = binding.switchAutoBackup.isChecked

            setFieldsEditable(false)
            Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show()
        }

        binding.switchAutoSms.setOnCheckedChangeListener { _, isChecked ->
            prefs.autoSmsEnabledGlobal = isChecked
            if (isChecked) {
                AutoSmsWorker.schedule(this, prefs.autoSmsHour, prefs.autoSmsMinute)
                Toast.makeText(this, "Auto SMS enabled", Toast.LENGTH_SHORT).show()
            } else {
                AutoSmsWorker.cancel(this)
                Toast.makeText(this, "Auto SMS disabled", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnPickSmsTime.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                prefs.autoSmsHour   = hour
                prefs.autoSmsMinute = minute
                updateSmsTimeDisplay()
                if (prefs.autoSmsEnabledGlobal) AutoSmsWorker.schedule(this, hour, minute)
            }, prefs.autoSmsHour, prefs.autoSmsMinute, false).show()
        }

        binding.btnBackupNow.setOnClickListener {
            lifecycleScope.launch {
                BackupManager.shareBackupViaGmail(this@SettingsActivity)
            }
        }

        binding.switchAutoBackup.setOnCheckedChangeListener { _, isChecked ->
            prefs.autoBackupEnabled = isChecked
        }

        // Clear SMS log
        binding.btnClearSmsLog.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear SMS History")
                .setMessage("Delete all SMS history?")
                .setPositiveButton("Clear") { _, _ ->
                    lifecycleScope.launch {
                        AppDatabase.getDatabase(this@SettingsActivity).smsLogDao().clearAll()
                    }
                }
                .setNegativeButton("Cancel", null).show()
        }
    }

    private fun updateSmsTimeDisplay() {
        val h = prefs.autoSmsHour
        val m = prefs.autoSmsMinute
        val amPm = if (h < 12) "AM" else "PM"
        val displayH = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
        binding.tvSmsTime.text = "Auto send at: %02d:%02d %s".format(displayH, m, amPm)
    }
}
