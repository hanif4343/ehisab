package com.rimon.my_e_khata.ui.cashbook

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rimon.my_e_khata.R
import com.rimon.my_e_khata.data.model.CashbookEntry
import com.rimon.my_e_khata.databinding.ItemCashbookBinding
import com.rimon.my_e_khata.utils.FormatUtils

class CashbookAdapter(
    private val onLongClick: (CashbookEntry) -> Unit
) : ListAdapter<CashbookEntry, CashbookAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemCashbookBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: CashbookEntry) {
            binding.tvDate.text = FormatUtils.formatDate(entry.createdAt)
            binding.tvBalance.text = "Bal. ৳ ${entry.balance}"
            binding.tvNote.text = entry.note.ifBlank { entry.category }

            if (entry.type == "in") {
                binding.tvAmountIn.text = "৳ ${entry.amount}"
                binding.tvAmountIn.setTextColor(ContextCompat.getColor(binding.root.context, R.color.green_settled))
                binding.tvAmountIn.visibility = android.view.View.VISIBLE
                binding.tvAmountOut.visibility = android.view.View.GONE
            } else {
                binding.tvAmountOut.text = "৳ ${entry.amount}"
                binding.tvAmountOut.setTextColor(ContextCompat.getColor(binding.root.context, R.color.red_due))
                binding.tvAmountOut.visibility = android.view.View.VISIBLE
                binding.tvAmountIn.visibility = android.view.View.GONE
            }

            binding.root.setOnLongClickListener { onLongClick(entry); true }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemCashbookBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<CashbookEntry>() {
        override fun areItemsTheSame(old: CashbookEntry, new: CashbookEntry) = old.id == new.id
        override fun areContentsTheSame(old: CashbookEntry, new: CashbookEntry) = old == new
    }
}
