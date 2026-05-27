package com.rimon.my_e_khata.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("my_ekhata_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_BUSINESS_NAME = "business_name"
        const val KEY_SMS_API_URL = "sms_api_url"
        const val KEY_SMS_API_KEY = "sms_api_key"
        const val KEY_SMS_SENDER_ID = "sms_sender_id"
        const val KEY_SMS_TEMPLATE_CUSTOMER = "sms_template_customer"
        const val KEY_SMS_TEMPLATE_SUPPLIER = "sms_template_supplier"
        const val KEY_AUTO_SMS_HOUR = "auto_sms_hour"
        const val KEY_AUTO_SMS_MINUTE = "auto_sms_minute"
        const val KEY_AUTO_SMS_ENABLED = "auto_sms_enabled_global"
        const val KEY_GMAIL_BACKUP_EMAIL = "gmail_backup_email"
        const val KEY_LAST_BACKUP_TIME = "last_backup_time"
        const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
        const val KEY_CURRENCY_SYMBOL = "currency_symbol"

        @Volatile
        private var instance: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return instance ?: synchronized(this) {
                AppPreferences(context.applicationContext).also { instance = it }
            }
        }
    }

    var businessName: String
        get() = prefs.getString(KEY_BUSINESS_NAME, "My Business") ?: "My Business"
        set(value) = prefs.edit { putString(KEY_BUSINESS_NAME, value) }

    var smsApiUrl: String
        get() = prefs.getString(KEY_SMS_API_URL, "") ?: ""
        set(value) = prefs.edit { putString(KEY_SMS_API_URL, value) }

    var smsApiKey: String
        get() = prefs.getString(KEY_SMS_API_KEY, "") ?: ""
        set(value) = prefs.edit { putString(KEY_SMS_API_KEY, value) }

    var smsSenderId: String
        get() = prefs.getString(KEY_SMS_SENDER_ID, "") ?: ""
        set(value) = prefs.edit { putString(KEY_SMS_SENDER_ID, value) }

    var smsTemplateCustomer: String
        get() = prefs.getString(KEY_SMS_TEMPLATE_CUSTOMER,
            "Dear {name}, you have a due of {amount} BDT. Please clear your balance. - {business}") ?: ""
        set(value) = prefs.edit { putString(KEY_SMS_TEMPLATE_CUSTOMER, value) }

    var smsTemplateSupplier: String
        get() = prefs.getString(KEY_SMS_TEMPLATE_SUPPLIER,
            "Dear {name}, you have a due of {amount} BDT. Please clear your balance. - {business}") ?: ""
        set(value) = prefs.edit { putString(KEY_SMS_TEMPLATE_SUPPLIER, value) }

    var autoSmsHour: Int
        get() = prefs.getInt(KEY_AUTO_SMS_HOUR, 10)
        set(value) = prefs.edit { putInt(KEY_AUTO_SMS_HOUR, value) }

    var autoSmsMinute: Int
        get() = prefs.getInt(KEY_AUTO_SMS_MINUTE, 0)
        set(value) = prefs.edit { putInt(KEY_AUTO_SMS_MINUTE, value) }

    var autoSmsEnabledGlobal: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SMS_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_SMS_ENABLED, value) }

    var gmailBackupEmail: String
        get() = prefs.getString(KEY_GMAIL_BACKUP_EMAIL, "") ?: ""
        set(value) = prefs.edit { putString(KEY_GMAIL_BACKUP_EMAIL, value) }

    var lastBackupTime: Long
        get() = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_BACKUP_TIME, value) }

    var autoBackupEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_BACKUP_ENABLED, value) }

    var currencySymbol: String
        get() = prefs.getString(KEY_CURRENCY_SYMBOL, "৳") ?: "৳"
        set(value) = prefs.edit { putString(KEY_CURRENCY_SYMBOL, value) }
}
