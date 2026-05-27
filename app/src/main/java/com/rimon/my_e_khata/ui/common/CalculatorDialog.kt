package com.rimon.my_e_khata.ui.common

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.rimon.my_e_khata.R

class CalculatorDialog(
    context: Context,
    private val onResult: (String) -> Unit
) : Dialog(context) {

    private lateinit var tvDisplay: TextView
    private var currentInput = ""
    private var operator = ""
    private var firstOperand = 0.0
    private var newInput = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_calculator)
        window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        tvDisplay = findViewById(R.id.tv_calc_display)
        setupButtons()
    }

    private fun setupButtons() {
        val btnIds = mapOf(
            R.id.btn_0 to "0", R.id.btn_1 to "1", R.id.btn_2 to "2",
            R.id.btn_3 to "3", R.id.btn_4 to "4", R.id.btn_5 to "5",
            R.id.btn_6 to "6", R.id.btn_7 to "7", R.id.btn_8 to "8",
            R.id.btn_9 to "9", R.id.btn_dot to "."
        )

        btnIds.forEach { (id, value) ->
            findViewById<Button>(id)?.setOnClickListener { appendDigit(value) }
        }

        findViewById<Button>(R.id.btn_plus)?.setOnClickListener { setOperator("+") }
        findViewById<Button>(R.id.btn_minus)?.setOnClickListener { setOperator("-") }
        findViewById<Button>(R.id.btn_multiply)?.setOnClickListener { setOperator("×") }
        findViewById<Button>(R.id.btn_divide)?.setOnClickListener { setOperator("÷") }
        findViewById<Button>(R.id.btn_equals)?.setOnClickListener { calculate() }
        findViewById<Button>(R.id.btn_clear)?.setOnClickListener { clear() }
        findViewById<Button>(R.id.btn_backspace)?.setOnClickListener { backspace() }
        findViewById<Button>(R.id.btn_use)?.setOnClickListener {
            val result = tvDisplay.text.toString()
            onResult(result)
            dismiss()
        }
        findViewById<Button>(R.id.btn_close)?.setOnClickListener { dismiss() }
    }

    private fun appendDigit(digit: String) {
        if (newInput) {
            currentInput = ""
            newInput = false
        }
        if (digit == "." && currentInput.contains(".")) return
        if (currentInput == "0" && digit != ".") currentInput = digit
        else currentInput += digit
        updateDisplay()
    }

    private fun setOperator(op: String) {
        firstOperand = currentInput.toDoubleOrNull() ?: 0.0
        operator = op
        newInput = true
    }

    private fun calculate() {
        val secondOperand = currentInput.toDoubleOrNull() ?: 0.0
        val result = when (operator) {
            "+" -> firstOperand + secondOperand
            "-" -> firstOperand - secondOperand
            "×" -> firstOperand * secondOperand
            "÷" -> if (secondOperand != 0.0) firstOperand / secondOperand else 0.0
            else -> secondOperand
        }
        currentInput = if (result == result.toLong().toDouble()) result.toLong().toString()
        else result.toString()
        operator = ""
        newInput = true
        updateDisplay()
    }

    private fun clear() {
        currentInput = ""
        operator = ""
        firstOperand = 0.0
        newInput = false
        tvDisplay.text = "0"
    }

    private fun backspace() {
        if (currentInput.isNotEmpty()) {
            currentInput = currentInput.dropLast(1)
            updateDisplay()
        }
    }

    private fun updateDisplay() {
        tvDisplay.text = currentInput.ifEmpty { "0" }
    }
}
