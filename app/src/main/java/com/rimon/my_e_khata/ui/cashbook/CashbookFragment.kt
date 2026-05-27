package com.rimon.my_e_khata.ui.cashbook

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.rimon.my_e_khata.R
import com.rimon.my_e_khata.databinding.FragmentCashbookBinding
import com.rimon.my_e_khata.utils.FormatUtils
import kotlinx.coroutines.launch

class CashbookFragment : Fragment() {

    private var _binding: FragmentCashbookBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CashbookViewModel by viewModels()
    private lateinit var adapter: CashbookAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCashbookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = CashbookAdapter { entry ->
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Entry")
                .setPositiveButton("Delete") { _, _ -> viewModel.deleteEntry(entry) }
                .setNegativeButton("Cancel", null).show()
        }
        binding.recyclerView.adapter = adapter

        viewModel.entries.observe(viewLifecycleOwner) { entries ->
            adapter.submitList(entries)
            binding.tvEmpty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.currentBalance.observe(viewLifecycleOwner) { balance ->
            binding.tvBalance.text = FormatUtils.formatAmount(balance)
            binding.tvBalance.setTextColor(
                requireContext().getColor(if (balance >= 0) R.color.green_settled else R.color.red_due)
            )
        }

        viewModel.totalIn.observe(viewLifecycleOwner) { binding.tvCashIn.text = FormatUtils.formatAmount(it) }
        viewModel.totalOut.observe(viewLifecycleOwner) { binding.tvCashOut.text = FormatUtils.formatAmount(it) }

        binding.btnCashIn.setOnClickListener { showAddDialog("in") }
        binding.btnCashOut.setOnClickListener { showAddDialog("out") }
    }

    private fun showAddDialog(type: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_cashbook, null)
        val etAmount = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_amount)
        val etNote = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_note)
        val etCategory = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_category)
        val btnCalc = dialogView.findViewById<android.widget.ImageButton>(R.id.btn_calculator)

        btnCalc?.setOnClickListener {
            com.rimon.my_e_khata.ui.common.CalculatorDialog(requireContext()) { result ->
                etAmount.setText(result)
            }.show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (type == "in") "Cash In" else "Cash Out")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val amount = FormatUtils.parseAmount(etAmount.text?.toString() ?: "")
                val note = etNote.text?.toString()?.trim() ?: ""
                val category = etCategory.text?.toString()?.trim() ?: ""
                if (amount > 0) {
                    viewModel.addEntry(type, amount, note, category)
                } else {
                    Toast.makeText(requireContext(), "Enter valid amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
