package com.rimon.my_e_khata.ui.suppliers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rimon.my_e_khata.R
import com.rimon.my_e_khata.data.model.Supplier
import com.rimon.my_e_khata.databinding.ItemCustomerBinding
import com.rimon.my_e_khata.utils.FormatUtils
import kotlin.math.abs

class SupplierAdapter(
    private val onClick: (Supplier) -> Unit
) : ListAdapter<Supplier, SupplierAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemCustomerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(supplier: Supplier) {
            binding.tvName.text = supplier.name
            binding.tvEntries.text = "• entries"
            val balance = supplier.balance
            when {
                balance > 0 -> {
                    binding.tvBalance.text = FormatUtils.formatAmount(balance)
                    binding.tvBalance.setTextColor(ContextCompat.getColor(binding.root.context, R.color.red_due))
                    binding.tvBalanceLabel.text = "You will give"
                }
                balance < 0 -> {
                    binding.tvBalance.text = FormatUtils.formatAmount(abs(balance))
                    binding.tvBalance.setTextColor(ContextCompat.getColor(binding.root.context, R.color.green_settled))
                    binding.tvBalanceLabel.text = "You will get"
                }
                else -> {
                    binding.tvBalance.text = FormatUtils.formatAmount(0.0)
                    binding.tvBalance.setTextColor(ContextCompat.getColor(binding.root.context, R.color.green_settled))
                    binding.tvBalanceLabel.text = "Settled Up"
                }
            }
            binding.root.setOnClickListener { onClick(supplier) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemCustomerBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<Supplier>() {
        override fun areItemsTheSame(old: Supplier, new: Supplier) = old.id == new.id
        override fun areContentsTheSame(old: Supplier, new: Supplier) = old == new
    }
}
