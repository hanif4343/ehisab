package com.rimon.my_e_khata.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rimon.my_e_khata.R
import com.rimon.my_e_khata.data.model.SmsLog
import com.rimon.my_e_khata.databinding.ItemSmsLogBinding
import com.rimon.my_e_khata.utils.FormatUtils

class SmsLogAdapter : ListAdapter<SmsLog, SmsLogAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val b: ItemSmsLogBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(log: SmsLog) {
            b.tvRecipientName.text   = log.recipientName
            b.tvRecipientNumber.text = log.recipientNumber
            b.tvMessage.text         = log.message
            b.tvSentAt.text          = FormatUtils.formatDate(log.sentAt)
            if (log.status == "sent") {
                b.tvStatus.text = "✓ Sent"
                b.tvStatus.setTextColor(ContextCompat.getColor(b.root.context, R.color.green_settled))
            } else {
                b.tvStatus.text = "✗ Failed"
                b.tvStatus.setTextColor(ContextCompat.getColor(b.root.context, R.color.red_due))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemSmsLogBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<SmsLog>() {
        override fun areItemsTheSame(o: SmsLog, n: SmsLog) = o.id == n.id
        override fun areContentsTheSame(o: SmsLog, n: SmsLog) = o == n
    }
}
