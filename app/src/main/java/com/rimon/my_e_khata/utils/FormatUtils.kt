package com.rimon.my_e_khata.utils

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

object FormatUtils {
    private val amountFormat = DecimalFormat("#,##0.00")

    fun formatAmount(amount: Double, symbol: String = "৳"): String {
        return "$symbol ${amountFormat.format(amount)}"
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yy · hh:mm a", Locale.ENGLISH)
        return sdf.format(Date(timestamp))
    }

    fun formatDateShort(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        return sdf.format(Date(timestamp))
    }

    fun formatYearMonth(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.ENGLISH)
        return sdf.format(Date(timestamp))
    }

    fun parseAmount(text: String): Double {
        return text.replace(",", "").trim().toDoubleOrNull() ?: 0.0
    }
}
