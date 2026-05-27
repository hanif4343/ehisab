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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.rimon.my_e_khata.data.db.AppDatabase
import com.rimon.my_e_khata.databinding.FragmentCustomersBinding
import com.rimon.my_e_khata.utils.AppPreferences
import com.rimon.my_e_khata.utils.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        adapter = CustomerAdapter(
            onClick = { customer ->
                startActivity(
                    Intent(requireContext(), CustomerDetailActivity::class.java)
                        .putExtra("customer_id", customer.id)
                )
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { viewModel.setSearchQuery(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun observeViewModel() {
        viewModel.customers.observe(viewLifecycleOwner) { customers ->
            adapter.submitList(customers)
            binding.tvEmpty.visibility = if (customers.isEmpty()) View.VISIBLE else View.GONE
            // Load entry counts for each customer
            val db = AppDatabase.getDatabase(requireContext())
            lifecycleScope.launch(Dispatchers.IO) {
                customers.forEach { c ->
                    val count = db.customerDao().getTransactionCount(c.id)
                    withContext(Dispatchers.Main) {
                        adapter.updateEntryCount(c.id, count)
                    }
                }
            }
        }
        viewModel.totalReceivable.observe(viewLifecycleOwner) { binding.tvWillGet.text  = FormatUtils.formatAmount(it) }
        viewModel.totalPayable.observe(viewLifecycleOwner)    { binding.tvWillGive.text = FormatUtils.formatAmount(it) }
    }

    private fun setupClickListeners() {
        binding.fabAddCustomer.setOnClickListener {
            startActivity(Intent(requireContext(), AddEditCustomerActivity::class.java))
        }
        binding.tvViewReport.setOnClickListener { /* full report */ }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshTotals()
        // Update business name from prefs
        binding.tvBusinessName.text = AppPreferences.getInstance(requireContext()).businessName
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
