package com.rimon.my_e_khata.ui.common

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.*
import com.rimon.my_e_khata.R
import com.rimon.my_e_khata.service.ReminderManager
import java.util.Calendar

class ReminderDialog(
    context: Context,
    private val customerId: Int,
    private val customerName: String,
    private val balanceText: String
) : Dialog(context) {

    private var selectedHour   = 10
    private var selectedMinute = 0
    private var selectedDay    = -1
    private var selectedMonth  = -1
    private var selectedYear   = -1
    private var repeatType     = ReminderManager.RepeatType.ONCE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_reminder)
        window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvSelectedDate   = findViewById<TextView>(R.id.tv_selected_date)
        val tvSelectedTime   = findViewById<TextView>(R.id.tv_selected_time)
        val btnPickDate      = findViewById<Button>(R.id.btn_pick_date)
        val btnPickTime      = findViewById<Button>(R.id.btn_pick_time)
        val rgRepeat         = findViewById<RadioGroup>(R.id.rg_repeat)
        val btnSetReminder   = findViewById<Button>(R.id.btn_set_reminder)
        val btnCancel        = findViewById<Button>(R.id.btn_cancel_reminder)

        // Default: today
        val now = Calendar.getInstance()
        selectedDay   = now.get(Calendar.DAY_OF_MONTH)
        selectedMonth = now.get(Calendar.MONTH)
        selectedYear  = now.get(Calendar.YEAR)
        updateDateDisplay(tvSelectedDate)
        updateTimeDisplay(tvSelectedTime)

        btnPickDate.setOnClickListener {
            DatePickerDialog(context, { _, y, m, d ->
                selectedYear = y; selectedMonth = m; selectedDay = d
                updateDateDisplay(tvSelectedDate)
            }, selectedYear, selectedMonth, selectedDay).show()
        }

        btnPickTime.setOnClickListener {
            TimePickerDialog(context, { _, h, min ->
                selectedHour = h; selectedMinute = min
                updateTimeDisplay(tvSelectedTime)
            }, selectedHour, selectedMinute, false).show()
        }

        rgRepeat.setOnCheckedChangeListener { _, id ->
            repeatType = when (id) {
                R.id.rb_once   -> ReminderManager.RepeatType.ONCE
                R.id.rb_daily  -> ReminderManager.RepeatType.DAILY
                R.id.rb_weekly -> ReminderManager.RepeatType.WEEKLY
                else           -> ReminderManager.RepeatType.ONCE
            }
        }

        btnSetReminder.setOnClickListener {
            ReminderManager.setReminder(
                context  = context,
                id       = customerId,
                title    = "Collection Reminder: $customerName",
                message  = "Due: $balanceText — Please collect payment from $customerName",
                hour     = selectedHour,
                minute   = selectedMinute,
                dayOfMonth = selectedDay,
                month    = selectedMonth,
                year     = selectedYear,
                repeat   = repeatType
            )
            Toast.makeText(context, "Reminder set for $customerName!", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        btnCancel.setOnClickListener { dismiss() }
    }

    private fun updateDateDisplay(tv: TextView) {
        tv.text = "%02d/%02d/%d".format(selectedDay, selectedMonth + 1, selectedYear)
    }

    private fun updateTimeDisplay(tv: TextView) {
        val amPm = if (selectedHour < 12) "AM" else "PM"
        val h    = when { selectedHour == 0 -> 12; selectedHour > 12 -> selectedHour - 12; else -> selectedHour }
        tv.text  = "%02d:%02d %s".format(h, selectedMinute, amPm)
    }
}
