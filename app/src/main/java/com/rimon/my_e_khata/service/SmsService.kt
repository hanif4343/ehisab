package com.rimon.my_e_khata.service

import android.content.Context
import android.util.Log
import com.rimon.my_e_khata.utils.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.FormBody
import java.net.URLEncoder

object SmsService {
    private val client = OkHttpClient()
    private const val TAG = "SmsService"

    suspend fun sendSms(
        context: Context,
        mobile: String,
        message: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val prefs = AppPreferences.getInstance(context)
        val apiUrl = prefs.smsApiUrl
        val apiKey = prefs.smsApiKey
        val senderId = prefs.smsSenderId

        if (apiUrl.isBlank()) {
            return@withContext Result.failure(Exception("SMS API URL not configured"))
        }

        try {
            // Build URL with params (common pattern for BD SMS APIs)
            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            val url = buildSmsUrl(apiUrl, apiKey, senderId, mobile, encodedMsg)

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                Log.d(TAG, "SMS sent to $mobile: $body")
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.code}: $body"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS to $mobile", e)
            Result.failure(e)
        }
    }

    private fun buildSmsUrl(
        apiUrl: String,
        apiKey: String,
        senderId: String,
        mobile: String,
        message: String
    ): String {
        // Supports common BD SMS gateway URL patterns
        // Users can configure their own URL with placeholders or direct URL
        return if (apiUrl.contains("{mobile}")) {
            apiUrl
                .replace("{api_key}", apiKey)
                .replace("{sender_id}", senderId)
                .replace("{mobile}", mobile)
                .replace("{message}", message)
        } else {
            // Append as query params
            "$apiUrl?api_key=$apiKey&sender_id=$senderId&to=$mobile&message=$message"
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
