package com.rimon.my_e_khata.ui.customers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rimon.my_e_khata.R
import com.rimon.my_e_khata.data.model.Customer
import com.rimon.my_e_khata.databinding.ItemCustomerBinding
import com.rimon.my_e_khata.utils.FormatUtils
import kotlin.math.abs

class CustomerAdapter(
    private val onClick: (Customer) -> Unit,
    private val onMenuClick: ((Customer) -> Unit)? = null
) : ListAdapter<Customer, CustomerAdapter.ViewHolder>(DiffCallback()) {

    // Map of customerId -> entry count, updated externally
    private val entryCounts = mutableMapOf<Long, Int>()

    fun updateEntryCount(customerId: Long, count: Int) {
        entryCounts[customerId] = count
        // find position and notify
        currentList.indexOfFirst { it.id == customerId }.takeIf { it >= 0 }?.let { notifyItemChanged(it) }
    }

    inner class ViewHolder(private val binding: ItemCustomerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(customer: Customer) {
            binding.tvName.text = customer.name
            val count = entryCounts[customer.id] ?: 0
            binding.tvEntries.text = "$count entries"

            val balance = customer.balance
            when {
                balance > 0 -> {
                    binding.tvBalance.text = FormatUtils.formatAmount(balance)
                    binding.tvBalance.setTextColor(ContextCompat.getColor(binding.root.context, R.color.red_due))
                    binding.tvBalanceLabel.text = "You will get"
                    binding.tvBalanceLabel.setTextColor(ContextCompat.getColor(binding.root.context, R.color.red_due))
                }
                balance < 0 -> {
                    binding.tvBalance.text = FormatUtils.formatAmount(abs(balance))
                    binding.tvBalance.setTextColor(ContextCompat.getColor(binding.root.context, R.color.green_settled))
                    binding.tvBalanceLabel.text = "You will give"
                    binding.tvBalanceLabel.setTextColor(ContextCompat.getColor(binding.root.context, R.color.text_secondary))
                }
                else -> {
                    binding.tvBalance.text = FormatUtils.formatAmount(0.0)
                    binding.tvBalance.setTextColor(ContextCompat.getColor(binding.root.context, R.color.green_settled))
                    binding.tvBalanceLabel.text = "Settled Up"
                    binding.tvBalanceLabel.setTextColor(ContextCompat.getColor(binding.root.context, R.color.green_settled))
                }
            }
            binding.root.setOnClickListener { onClick(customer) }
            binding.root.setOnLongClickListener { onMenuClick?.invoke(customer); true }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCustomerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Customer>() {
        override fun areItemsTheSame(old: Customer, new: Customer) = old.id == new.id
        override fun areContentsTheSame(old: Customer, new: Customer) = old == new
    }
}
