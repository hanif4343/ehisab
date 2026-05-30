package com.rimon.my_e_khata.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.rimon.my_e_khata.R
import java.util.Calendar

object ReminderManager {

    const val EXTRA_TITLE   = "reminder_title"
    const val EXTRA_MESSAGE = "reminder_message"
    const val EXTRA_ID      = "reminder_id"

    enum class RepeatType { ONCE, DAILY, WEEKLY }

    fun setReminder(
        context: Context,
        id: Int,
        title: String,
        message: String,
        hour: Int,
        minute: Int,
        dayOfMonth: Int = -1,  // for ONCE: specific day
        month: Int     = -1,
        year: Int      = -1,
        repeat: RepeatType = RepeatType.ONCE
    ) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_ID, id)
        }
        val pi = PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (dayOfMonth > 0 && month >= 0 && year > 0) {
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.MONTH, month)
                set(Calendar.YEAR, year)
            }
            // If time already passed today, move to next occurrence
            if (before(Calendar.getInstance())) {
                when (repeat) {
                    RepeatType.ONCE    -> add(Calendar.DAY_OF_MONTH, 1)
                    RepeatType.DAILY   -> add(Calendar.DAY_OF_MONTH, 1)
                    RepeatType.WEEKLY  -> add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
        }

        when (repeat) {
            RepeatType.ONCE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
                }
            }
            RepeatType.DAILY -> {
                am.setRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pi)
            }
            RepeatType.WEEKLY -> {
                am.setRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_DAY * 7, pi)
            }
        }
    }

    fun cancelReminder(context: Context, id: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context, id, Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title   = intent.getStringExtra(ReminderManager.EXTRA_TITLE)   ?: "Reminder"
        val message = intent.getStringExtra(ReminderManager.EXTRA_MESSAGE) ?: ""
        val id      = intent.getIntExtra(ReminderManager.EXTRA_ID, 0)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "reminder_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Reminders", NotificationManager.IMPORTANCE_HIGH)
            )
        }

        nm.notify(id + 2000, NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        )
    }
}
