package com.rimon.my_e_khata.ui.settings

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.rimon.my_e_khata.databinding.ActivitySettingsBinding
import com.rimon.my_e_khata.service.AutoSmsWorker
import com.rimon.my_e_khata.utils.AppPreferences
import com.rimon.my_e_khata.utils.BackupManager
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = AppPreferences.getInstance(this)
        binding.toolbar.setNavigationOnClickListener { finish() }

        loadSettings()
        setupListeners()
    }

    private fun loadSettings() {
        binding.etBusinessName.setText(prefs.businessName)
        binding.etSmsApiUrl.setText(prefs.smsApiUrl)
        binding.etSmsApiKey.setText(prefs.smsApiKey)
        binding.etSmsSenderId.setText(prefs.smsSenderId)
        binding.etSmsTemplateCustomer.setText(prefs.smsTemplateCustomer)
        binding.etSmsTemplateSupplier.setText(prefs.smsTemplateSupplier)
        binding.switchAutoSms.isChecked = prefs.autoSmsEnabledGlobal
        updateSmsTimeDisplay()
        binding.etGmailEmail.setText(prefs.gmailBackupEmail)
        binding.switchAutoBackup.isChecked = prefs.autoBackupEnabled
        binding.etCurrencySymbol.setText(prefs.currencySymbol)
    }

    private fun setupListeners() {
        binding.btnSaveGeneral.setOnClickListener {
            prefs.businessName = binding.etBusinessName.text?.toString()?.trim() ?: "My Business"
            prefs.currencySymbol = binding.etCurrencySymbol.text?.toString()?.trim() ?: "৳"
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
        }

        binding.btnSaveSmsApi.setOnClickListener {
            prefs.smsApiUrl = binding.etSmsApiUrl.text?.toString()?.trim() ?: ""
            prefs.smsApiKey = binding.etSmsApiKey.text?.toString()?.trim() ?: ""
            prefs.smsSenderId = binding.etSmsSenderId.text?.toString()?.trim() ?: ""
            prefs.smsTemplateCustomer = binding.etSmsTemplateCustomer.text?.toString()?.trim() ?: prefs.smsTemplateCustomer
            prefs.smsTemplateSupplier = binding.etSmsTemplateSupplier.text?.toString()?.trim() ?: prefs.smsTemplateSupplier
            Toast.makeText(this, "SMS API settings saved!", Toast.LENGTH_SHORT).show()
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
                prefs.autoSmsHour = hour
                prefs.autoSmsMinute = minute
                updateSmsTimeDisplay()
                if (prefs.autoSmsEnabledGlobal) {
                    AutoSmsWorker.schedule(this, hour, minute)
                }
            }, prefs.autoSmsHour, prefs.autoSmsMinute, false).show()
        }

        binding.btnBackupNow.setOnClickListener {
            prefs.gmailBackupEmail = binding.etGmailEmail.text?.toString()?.trim() ?: ""
            lifecycleScope.launch {
                BackupManager.shareBackupViaGmail(this@SettingsActivity)
            }
        }

        binding.switchAutoBackup.setOnCheckedChangeListener { _, isChecked ->
            prefs.autoBackupEnabled = isChecked
            // Schedule daily backup via WorkManager
            if (isChecked) {
                scheduleAutoBackup()
            }
        }
    }

    private fun updateSmsTimeDisplay() {
        val hour = prefs.autoSmsHour
        val minute = prefs.autoSmsMinute
        val amPm = if (hour < 12) "AM" else "PM"
        val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        binding.tvSmsTime.text = "Auto send at: %02d:%02d %s".format(h, minute, amPm)
    }

    private fun scheduleAutoBackup() {
        // Schedule daily backup via WorkManager
        val request = androidx.work.PeriodicWorkRequestBuilder<BackupWorker>(1, java.util.concurrent.TimeUnit.DAYS).build()
        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "auto_backup", androidx.work.ExistingPeriodicWorkPolicy.REPLACE, request
        )
    }
}
