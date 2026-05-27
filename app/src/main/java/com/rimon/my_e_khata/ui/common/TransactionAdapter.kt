package com.rimon.my_e_khata.ui.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rimon.my_e_khata.R
import com.rimon.my_e_khata.data.model.Transaction
import com.rimon.my_e_khata.databinding.ItemTransactionBinding
import com.rimon.my_e_khata.utils.FormatUtils

class TransactionAdapter(
    private val currencySymbol: String = "৳",
    private val onDelete: ((Transaction) -> Unit)? = null
) : ListAdapter<Transaction, TransactionAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(tx: Transaction) {
            binding.tvDate.text = FormatUtils.formatDate(tx.createdAt)
            binding.tvBalance.text = "Bal. $currencySymbol ${tx.balance}"

            if (tx.note.isNotBlank()) {
                binding.tvNote.text = tx.note
                binding.tvNote.visibility = View.VISIBLE
            } else {
                binding.tvNote.visibility = View.GONE
            }

            if (tx.type == "gave") {
                binding.tvAmountGave.text = "$currencySymbol ${tx.amount}"
                binding.tvAmountGave.setTextColor(ContextCompat.getColor(binding.root.context, R.color.red_due))
                binding.tvAmountGave.visibility = View.VISIBLE
                binding.tvAmountGot.visibility  = View.GONE
            } else {
                binding.tvAmountGot.text = "$currencySymbol ${tx.amount}"
                binding.tvAmountGot.setTextColor(ContextCompat.getColor(binding.root.context, R.color.green_settled))
                binding.tvAmountGot.visibility  = View.VISIBLE
                binding.tvAmountGave.visibility = View.GONE
            }

            binding.root.setOnLongClickListener {
                onDelete?.invoke(tx)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(old: Transaction, new: Transaction) = old.id == new.id
        override fun areContentsTheSame(old: Transaction, new: Transaction) = old == new
    }
}
