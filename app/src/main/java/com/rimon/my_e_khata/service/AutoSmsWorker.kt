package com.rimon.my_e_khata.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.rimon.my_e_khata.R
import com.rimon.my_e_khata.data.db.AppDatabase
import com.rimon.my_e_khata.utils.AppPreferences
import com.rimon.my_e_khata.utils.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class AutoSmsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = AppPreferences.getInstance(applicationContext)
        if (!prefs.autoSmsEnabledGlobal) return@withContext Result.success()

        val db = AppDatabase.getDatabase(applicationContext)
        val template = prefs.smsTemplateCustomer
        val businessName = prefs.businessName

        var sentCount = 0
        var failCount = 0

        // Send to customers with due
        val customers = db.customerDao().getCustomersForAutoSms()
        for (customer in customers) {
            val message = SmsService.buildMessage(
                template,
                customer.name,
                FormatUtils.formatAmount(customer.balance, ""),
                businessName
            )
            val result = SmsService.sendSms(applicationContext, customer.name, customer.mobile, message)
            if (result.isSuccess) sentCount++ else failCount++
        }

        // Send to suppliers with due
        val supplierTemplate = prefs.smsTemplateSupplier
        val suppliers = db.supplierDao().getSuppliersForAutoSms()
        for (supplier in suppliers) {
            val message = SmsService.buildMessage(
                supplierTemplate,
                supplier.name,
                FormatUtils.formatAmount(supplier.balance, ""),
                businessName
            )
            val result = SmsService.sendSms(applicationContext, supplier.name, supplier.mobile, message)
            if (result.isSuccess) sentCount++ else failCount++
        }

        showNotification("Auto SMS", "Sent: $sentCount, Failed: $failCount")
        Result.success()
    }

    private fun showNotification(title: String, message: String) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "auto_sms_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Auto SMS", NotificationManager.IMPORTANCE_DEFAULT)
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .build()

        nm.notify(1001, notification)
    }

    companion object {
        const val WORK_NAME = "auto_sms_work"

        fun schedule(context: Context, hour: Int, minute: Int) {
            val now = java.util.Calendar.getInstance()
            val target = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                if (before(now)) add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
            val delay = target.timeInMillis - now.timeInMillis

            val request = PeriodicWorkRequestBuilder<AutoSmsWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
