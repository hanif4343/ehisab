package com.rimon.my_e_khata.service

import android.content.Context
import android.util.Log
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
        mobile: String,
        message: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val prefs = AppPreferences.getInstance(context)
        val apiUrl = prefs.smsApiUrl.trim()
        val apiKey = prefs.smsApiKey.trim()

        if (apiUrl.isBlank()) {
            return@withContext Result.failure(Exception("SMS API URL not configured. Go to Settings."))
        }
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("SMS API Key not configured. Go to Settings."))
        }
        if (mobile.isBlank()) {
            return@withContext Result.failure(Exception("No mobile number"))
        }

        try {
            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            val cleanMobile = mobile.replace("+", "").replace("-", "").replace(" ", "")
            val url = buildUrl(apiUrl, apiKey, cleanMobile, encodedMsg)

            Log.d(TAG, "Sending SMS to $cleanMobile via $url")

            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            Log.d(TAG, "SMS response [${response.code}]: $body")

            if (response.isSuccessful) {
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.code}: $body"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "SMS failed to $mobile", e)
            Result.failure(e)
        }
    }

    /**
     * Builds the SMS API URL.
     *
     * Supported formats:
     * 1. demosoftpp style: https://demosoftpp.com/api.php?type=sms&key=KEY&number=NUMBER&msg=MSG
     *    → stored as: https://demosoftpp.com/api.php?type=sms
     *    → we append: &key={apiKey}&number={mobile}&msg={message}
     *
     * 2. Custom placeholders: url contains {key}, {number}, {msg}
     *    → we replace them
     *
     * 3. Fallback: append &key=&number=&msg= params
     */
    private fun buildUrl(apiUrl: String, apiKey: String, mobile: String, encodedMsg: String): String {
        return when {
            // Has custom placeholders
            apiUrl.contains("{number}") || apiUrl.contains("{mobile}") -> {
                apiUrl
                    .replace("{key}", apiKey)
                    .replace("{api_key}", apiKey)
                    .replace("{number}", mobile)
                    .replace("{mobile}", mobile)
                    .replace("{msg}", encodedMsg)
                    .replace("{message}", encodedMsg)
            }
            // demosoftpp style or any URL that already has query params
            apiUrl.contains("?") -> {
                "$apiUrl&key=$apiKey&number=$mobile&msg=$encodedMsg"
            }
            // Plain base URL, no params yet
            else -> {
                "$apiUrl?key=$apiKey&number=$mobile&msg=$encodedMsg"
            }
        }
    }

    fun buildMessage(
        template: String,
        name: String,
        amount: String,
        businessName: String
    ): String {
        return template
            .replace("{name}", name)
            .replace("{amount}", amount)
            .replace("{business}", businessName)
    }
}
