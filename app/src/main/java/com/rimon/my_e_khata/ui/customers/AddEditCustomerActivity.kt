package com.rimon.my_e_khata.ui.customers

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.rimon.my_e_khata.data.model.Customer
import com.rimon.my_e_khata.databinding.ActivityAddEditCustomerBinding

class AddEditCustomerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditCustomerBinding
    private lateinit var viewModel: CustomerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditCustomerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[CustomerViewModel::class.java]
        binding.toolbar.title = "Add Customer"
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Observe navigateBack event — finish when DB insert is done
        viewModel.navigateBack.observe(this) { shouldGo ->
            if (shouldGo) {
                viewModel.resetNavigateBack()
                Toast.makeText(this, "Customer saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        binding.btnImportContact.setOnClickListener { /* contact picker */ }

        binding.btnDone.setOnClickListener {
            val name   = binding.etName.text?.toString()?.trim() ?: ""
            val mobile = binding.etMobile.text?.toString()?.trim() ?: ""

            if (name.isBlank()) {
                binding.etName.error = "Name is required"
                return@setOnClickListener
            }
            if (mobile.isBlank()) {
                binding.etMobile.error = "Mobile number is required"
                return@setOnClickListener
            }

            binding.btnDone.isEnabled = false
            viewModel.addCustomer(
                Customer(
                    name = name,
                    mobile = mobile,
                    email   = binding.etEmail.text?.toString()?.trim()   ?: "",
                    address = binding.etAddress.text?.toString()?.trim() ?: "",
                    governmentId = binding.etGovId.text?.toString()?.trim() ?: ""
                )
            )
        }
    }
}
