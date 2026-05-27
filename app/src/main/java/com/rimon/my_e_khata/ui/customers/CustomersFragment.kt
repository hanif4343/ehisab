package com.rimon.my_e_khata.ui.customers

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.rimon.my_e_khata.databinding.FragmentCustomersBinding
import com.rimon.my_e_khata.utils.FormatUtils

class CustomersFragment : Fragment() {

    private var _binding: FragmentCustomersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CustomerViewModel by viewModels()
    private lateinit var adapter: CustomerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCustomersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        observeViewModel()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        adapter = CustomerAdapter { customer ->
            val intent = Intent(requireContext(), CustomerDetailActivity::class.java)
            intent.putExtra("customer_id", customer.id)
            startActivity(intent)
        }
        binding.recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun observeViewModel() {
        viewModel.customers.observe(viewLifecycleOwner) { customers ->
            adapter.submitList(customers)
            binding.tvEmpty.visibility = if (customers.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.totalReceivable.observe(viewLifecycleOwner) { amount ->
            binding.tvWillGet.text = FormatUtils.formatAmount(amount)
        }

        viewModel.totalPayable.observe(viewLifecycleOwner) { amount ->
            binding.tvWillGive.text = FormatUtils.formatAmount(amount)
        }
    }

    private fun setupClickListeners() {
        binding.fabAddCustomer.setOnClickListener {
            val intent = Intent(requireContext(), AddEditCustomerActivity::class.java)
            startActivity(intent)
        }

        binding.tvViewReport.setOnClickListener {
            // Open full report
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshTotals()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
