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

class AutoSmsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = AppPreferences.getInstance(applicationContext)
        if (!prefs.autoSmsEnabledGlobal) return@withContext Result.success()

        val db = AppDatabase.getDatabase(applicationContext)
        val template     = prefs.smsTemplateCustomer
        val businessName = prefs.businessName

        var sentCount = 0; var failCount = 0

        val customers = db.customerDao().getCustomersForAutoSms()
        for (c in customers) {
            val msg = SmsService.buildMessage(template, c.name, FormatUtils.formatAmount(c.balance, ""), businessName)
            val r   = SmsService.sendSms(applicationContext, c.name, c.mobile, msg)
            if (r.isSuccess) sentCount++ else failCount++
        }

        showNotification("Auto SMS", "Sent: $sentCount, Failed: $failCount")
        Result.success()
    }

    private fun showNotification(title: String, message: String) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "auto_sms_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(channelId, "Auto SMS", NotificationManager.IMPORTANCE_DEFAULT))
        }
        nm.notify(1001, NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title).setContentText(message)
            .setSmallIcon(R.drawable.ic_notification).setAutoCancel(true).build())
    }

    companion object {
        const val WORK_NAME = "auto_sms_daily"

        fun schedule(context: Context, hour: Int, minute: Int) {
            val now    = java.util.Calendar.getInstance()
            val target = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                if (!after(now)) add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
            val delay = target.timeInMillis - now.timeInMillis

            val request = PeriodicWorkRequestBuilder<AutoSmsWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())
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
