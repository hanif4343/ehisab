package com.rimon.my_e_khata.ui.suppliers

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.rimon.my_e_khata.data.model.Supplier
import com.rimon.my_e_khata.databinding.ActivityAddEditCustomerBinding

class AddEditSupplierActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddEditCustomerBinding
    private lateinit var viewModel: SupplierViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditCustomerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[SupplierViewModel::class.java]
        binding.toolbar.title = "Add Supplier"
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnDone.setOnClickListener {
            val name = binding.etName.text?.toString()?.trim() ?: ""
            if (name.isBlank()) { binding.etName.error = "Name required"; return@setOnClickListener }
            viewModel.addSupplier(Supplier(
                name = name,
                mobile = binding.etMobile.text?.toString()?.trim() ?: "",
                email = binding.etEmail.text?.toString()?.trim() ?: "",
                address = binding.etAddress.text?.toString()?.trim() ?: "",
                governmentId = binding.etGovId.text?.toString()?.trim() ?: ""
            )) { finish() }
        }
    }
}
