package com.rimon.my_e_khata.service

import android.content.Context
import android.util.Log
import com.rimon.my_e_khata.data.db.AppDatabase
import com.rimon.my_e_khata.data.model.SmsLog
import com.rimon.my_e_khata.utils.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object SmsService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private const val TAG = "SmsService"

    suspend fun sendSms(
        context: Context,
        recipientName: String,
        mobile: String,
        message: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val prefs = AppPreferences.getInstance(context)
        val apiUrl = prefs.smsApiUrl.trim()
        val apiKey = prefs.smsApiKey.trim()

        if (apiUrl.isBlank()) return@withContext Result.failure(Exception("SMS API URL not configured"))
        if (apiKey.isBlank()) return@withContext Result.failure(Exception("SMS API Key not configured"))
        if (mobile.isBlank()) return@withContext Result.failure(Exception("No mobile number"))

        val cleanMobile = mobile.replace("+", "").replace("-", "").replace(" ", "")
        val encodedMsg = URLEncoder.encode(message, "UTF-8")
        val url = buildUrl(apiUrl, apiKey, cleanMobile, encodedMsg)

        Log.d(TAG, "Sending SMS to $cleanMobile")

        val status: String
        val result: Result<String>
        try {
            val response = client.newCall(Request.Builder().url(url).get().build()).execute()
            val body = response.body?.string() ?: ""
            status = if (response.isSuccessful) "sent" else "failed"
            result = if (response.isSuccessful) Result.success(body)
                     else Result.failure(Exception("HTTP ${response.code}: $body"))
        } catch (e: Exception) {
            Log.e(TAG, "SMS failed", e)
            // Log as failed
            AppDatabase.getDatabase(context).smsLogDao().insert(
                SmsLog(recipientName = recipientName, recipientNumber = cleanMobile,
                       message = message, status = "failed")
            )
            return@withContext Result.failure(e)
        }

        // Save to log
        AppDatabase.getDatabase(context).smsLogDao().insert(
            SmsLog(recipientName = recipientName, recipientNumber = cleanMobile,
                   message = message, status = status)
        )
        result
    }

    private fun buildUrl(apiUrl: String, apiKey: String, mobile: String, encodedMsg: String): String {
        return when {
            apiUrl.contains("{number}") || apiUrl.contains("{mobile}") ->
                apiUrl.replace("{key}", apiKey).replace("{api_key}", apiKey)
                    .replace("{number}", mobile).replace("{mobile}", mobile)
                    .replace("{msg}", encodedMsg).replace("{message}", encodedMsg)
            apiUrl.contains("?") -> "$apiUrl&key=$apiKey&number=$mobile&msg=$encodedMsg"
            else -> "$apiUrl?key=$apiKey&number=$mobile&msg=$encodedMsg"
        }
    }

    fun buildMessage(template: String, name: String, amount: String, businessName: String): String {
        return template.replace("{name}", name).replace("{amount}", amount).replace("{business}", businessName)
    }
}
