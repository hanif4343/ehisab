package com.rimon.my_e_khata.ui.customers

import android.os.Bundle
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

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.title = "Add Customer"

        binding.btnImportContact.setOnClickListener {
            // Launch contact picker
        }

        binding.btnDone.setOnClickListener {
            val name = binding.etName.text?.toString()?.trim() ?: ""
            if (name.isBlank()) {
                binding.etName.error = "Name is required"
                return@setOnClickListener
            }

            val customer = Customer(
                name = name,
                mobile = binding.etMobile.text?.toString()?.trim() ?: "",
                email = binding.etEmail.text?.toString()?.trim() ?: "",
                address = binding.etAddress.text?.toString()?.trim() ?: "",
                governmentId = binding.etGovId.text?.toString()?.trim() ?: ""
            )

            viewModel.addCustomer(customer) {
                finish()
            }
        }
    }
}
