package com.rimon.my_e_khata.ui.suppliers

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.rimon.my_e_khata.databinding.FragmentSuppliersBinding
import com.rimon.my_e_khata.utils.FormatUtils

class SuppliersFragment : Fragment() {

    private var _binding: FragmentSuppliersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SupplierViewModel by viewModels()
    private lateinit var adapter: SupplierAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSuppliersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SupplierAdapter { supplier ->
            val intent = Intent(requireContext(), SupplierDetailActivity::class.java)
            intent.putExtra("supplier_id", supplier.id)
            startActivity(intent)
        }
        binding.recyclerView.adapter = adapter

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { viewModel.setSearchQuery(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        viewModel.suppliers.observe(viewLifecycleOwner) { suppliers ->
            adapter.submitList(suppliers)
            binding.tvEmpty.visibility = if (suppliers.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.totalPayable.observe(viewLifecycleOwner) { binding.tvWillGive.text = FormatUtils.formatAmount(it) }
        viewModel.totalReceivable.observe(viewLifecycleOwner) { binding.tvWillGet.text = FormatUtils.formatAmount(it) }

        binding.fabAddSupplier.setOnClickListener {
            startActivity(Intent(requireContext(), AddEditSupplierActivity::class.java))
        }
    }

    override fun onResume() { super.onResume(); viewModel.refreshTotals() }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
