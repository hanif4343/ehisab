package com.rimon.my_e_khata.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.rimon.my_e_khata.R
import com.rimon.my_e_khata.data.db.AppDatabase
import kotlinx.coroutines.launch

class SmsHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms_history)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val recycler   = findViewById<RecyclerView>(R.id.recycler_sms_log)
        val tvEmpty    = findViewById<TextView>(R.id.tv_sms_log_empty)
        val tvCount    = findViewById<TextView>(R.id.tv_sms_log_count)
        val btnClear   = findViewById<MaterialButton>(R.id.btn_clear_sms_log)

        val adapter = SmsLogAdapter()
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        AppDatabase.getDatabase(this).smsLogDao().getAllLogs().observe(this) { logs ->
            adapter.submitList(logs)
            tvEmpty.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
            tvCount.text = "${logs.size} messages sent"
        }

        btnClear.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear SMS History")
                .setMessage("Delete all SMS history?")
                .setPositiveButton("Clear") { _, _ ->
                    lifecycleScope.launch {
                        AppDatabase.getDatabase(this@SmsHistoryActivity).smsLogDao().clearAll()
                    }
                }
                .setNegativeButton("Cancel", null).show()
        }
    }
}
