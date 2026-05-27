package com.rimon.my_e_khata.ui.settings

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rimon.my_e_khata.utils.BackupManager

class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            BackupManager.shareBackupViaGmail(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
