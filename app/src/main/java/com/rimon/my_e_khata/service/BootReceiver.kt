package com.rimon.my_e_khata.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rimon.my_e_khata.utils.AppPreferences

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = AppPreferences.getInstance(context)
            if (prefs.autoSmsEnabledGlobal) {
                AutoSmsWorker.schedule(context, prefs.autoSmsHour, prefs.autoSmsMinute)
            }
        }
    }
}
