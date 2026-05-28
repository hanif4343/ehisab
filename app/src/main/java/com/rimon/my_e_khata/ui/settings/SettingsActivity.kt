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
    private var isEditing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = AppPreferences.getInstance(this)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupSmsLogRecycler()
        loadValues()
        setEditing(false)   // start read-only
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

    private fun loadValues() {
        binding.etBusinessName.setText(prefs.businessName)
        binding.etCurrencySymbol.setText(prefs.currencySymbol)
        binding.etSmsApiUrl.setText(prefs.smsApiUrl)
        binding.etSmsApiKey.setText(prefs.smsApiKey)
        binding.etSmsSenderId.setText(prefs.smsSenderId)
        binding.etSmsTemplateCustomer.setText(prefs.smsTemplateCustomer)
        binding.switchAutoSms.isChecked = prefs.autoSmsEnabledGlobal
        updateSmsTimeDisplay()
        binding.etGmailEmail.setText(prefs.gmailBackupEmail)
        binding.switchAutoBackup.isChecked = prefs.autoBackupEnabled
    }

    private fun setEditing(editing: Boolean) {
        isEditing = editing
        val fields = listOf(
            binding.etBusinessName, binding.etCurrencySymbol,
            binding.etSmsApiUrl, binding.etSmsApiKey, binding.etSmsSenderId,
            binding.etSmsTemplateCustomer, binding.etGmailEmail
        )
        fields.forEach {
            it.isEnabled = editing
            it.isFocusable = editing
            it.isFocusableInTouchMode = editing
        }
        binding.btnEditSettings.text = if (editing) "✕ Cancel" else "✎ Edit"
        binding.btnSaveAll.visibility = if (editing) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun saveAll() {
        val url = binding.etSmsApiUrl.text?.toString()?.trim() ?: ""
        val key = binding.etSmsApiKey.text?.toString()?.trim() ?: ""

        prefs.businessName          = binding.etBusinessName.text?.toString()?.trim()?.ifBlank { "My Business" } ?: "My Business"
        prefs.currencySymbol        = binding.etCurrencySymbol.text?.toString()?.trim()?.ifBlank { "৳" } ?: "৳"
        prefs.smsApiUrl             = url
        prefs.smsApiKey             = key
        prefs.smsSenderId           = binding.etSmsSenderId.text?.toString()?.trim() ?: ""
        prefs.smsTemplateCustomer   = binding.etSmsTemplateCustomer.text?.toString()?.trim()
            .takeIf { it?.isNotBlank() == true } ?: prefs.smsTemplateCustomer
        prefs.gmailBackupEmail      = binding.etGmailEmail.text?.toString()?.trim() ?: ""
        prefs.autoBackupEnabled     = binding.switchAutoBackup.isChecked

        setEditing(false)

        val msg = if (url.isBlank() || key.isBlank())
            "Saved! (SMS won't work — API URL/Key missing)"
        else
            "✓ Settings saved!"
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun setupListeners() {
        binding.btnEditSettings.setOnClickListener {
            if (isEditing) {
                loadValues()        // cancel — restore
                setEditing(false)
            } else {
                setEditing(true)
                binding.etSmsApiUrl.requestFocus()
            }
        }

        binding.btnSaveAll.setOnClickListener { saveAll() }

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
                Toast.makeText(this, "Auto SMS time updated", Toast.LENGTH_SHORT).show()
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

        binding.btnClearSmsLog.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear SMS History")
                .setMessage("Delete all SMS history?")
                .setPositiveButton("Clear") { _, _ ->
                    lifecycleScope.launch {
                        AppDatabase.getDatabase(this@SettingsActivity).smsLogDao().clearAll()
                        Toast.makeText(this@SettingsActivity, "SMS history cleared", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null).show()
        }
    }

    private fun updateSmsTimeDisplay() {
        val h = prefs.autoSmsHour; val m = prefs.autoSmsMinute
        val amPm = if (h < 12) "AM" else "PM"
        val displayH = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
        binding.tvSmsTime.text = "Auto send at: %02d:%02d %s".format(displayH, m, amPm)
    }
}
