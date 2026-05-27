package com.rimon.my_e_khata.utils

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import com.rimon.my_e_khata.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object BackupManager {

    suspend fun generateBackupCsv(context: Context): File = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val file = File(dir, "ekhata_backup_$timestamp.csv")

        FileWriter(file).use { writer ->
            writer.write("=== MY E-KHATA BACKUP ===\n")
            writer.write("Generated: ${FormatUtils.formatDateShort(System.currentTimeMillis())}\n\n")

            // Customers
            writer.write("CUSTOMERS\n")
            writer.write("ID,Name,Mobile,Email,Address,Balance,AutoSMS\n")
            // We need a synchronous query; use a coroutine-friendly approach
            val customerJob = kotlinx.coroutines.async(Dispatchers.IO) {
                // Direct DB access on IO thread
                db.openHelper.readableDatabase.let { dbRaw ->
                    val rows = mutableListOf<String>()
                    val cursor = dbRaw.rawQuery("SELECT id,name,mobile,email,address,balance,autoSmsEnabled FROM customers ORDER BY id", null)
                    while (cursor.moveToNext()) {
                        rows.add("${cursor.getLong(0)},\"${cursor.getString(1)}\",${cursor.getString(2)},${cursor.getString(3)},\"${cursor.getString(4)}\",${cursor.getDouble(5)},${cursor.getInt(6)}")
                    }
                    cursor.close()
                    rows
                }
            }

            val supplierJob = kotlinx.coroutines.async(Dispatchers.IO) {
                val rows = mutableListOf<String>()
                val cursor = db.openHelper.readableDatabase.rawQuery("SELECT id,name,mobile,email,address,balance,autoSmsEnabled FROM suppliers ORDER BY id", null)
                while (cursor.moveToNext()) {
                    rows.add("${cursor.getLong(0)},\"${cursor.getString(1)}\",${cursor.getString(2)},${cursor.getString(3)},\"${cursor.getString(4)}\",${cursor.getDouble(5)},${cursor.getInt(6)}")
                }
                cursor.close()
                rows
            }

            val txJob = kotlinx.coroutines.async(Dispatchers.IO) {
                val rows = mutableListOf<String>()
                val cursor = db.openHelper.readableDatabase.rawQuery("SELECT id,partyId,partyType,type,amount,balance,note,createdAt FROM transactions ORDER BY createdAt", null)
                while (cursor.moveToNext()) {
                    rows.add("${cursor.getLong(0)},${cursor.getLong(1)},${cursor.getString(2)},${cursor.getString(3)},${cursor.getDouble(4)},${cursor.getDouble(5)},\"${cursor.getString(6)}\",${cursor.getLong(7)}")
                }
                cursor.close()
                rows
            }

            val cbJob = kotlinx.coroutines.async(Dispatchers.IO) {
                val rows = mutableListOf<String>()
                val cursor = db.openHelper.readableDatabase.rawQuery("SELECT id,type,amount,balance,category,note,createdAt FROM cashbook ORDER BY createdAt", null)
                while (cursor.moveToNext()) {
                    rows.add("${cursor.getLong(0)},${cursor.getString(1)},${cursor.getDouble(2)},${cursor.getDouble(3)},\"${cursor.getString(4)}\",\"${cursor.getString(5)}\",${cursor.getLong(6)}")
                }
                cursor.close()
                rows
            }

            val customers = customerJob.await()
            val suppliers = supplierJob.await()
            val transactions = txJob.await()
            val cashbook = cbJob.await()

            customers.forEach { writer.write("$it\n") }

            writer.write("\nSUPPLIERS\n")
            writer.write("ID,Name,Mobile,Email,Address,Balance,AutoSMS\n")
            suppliers.forEach { writer.write("$it\n") }

            writer.write("\nTRANSACTIONS\n")
            writer.write("ID,PartyID,PartyType,Type,Amount,Balance,Note,CreatedAt\n")
            transactions.forEach { writer.write("$it\n") }

            writer.write("\nCASHBOOK\n")
            writer.write("ID,Type,Amount,Balance,Category,Note,CreatedAt\n")
            cashbook.forEach { writer.write("$it\n") }
        }

        AppPreferences.getInstance(context).lastBackupTime = System.currentTimeMillis()
        file
    }

    suspend fun shareBackupViaGmail(context: Context) {
        val file = generateBackupCsv(context)
        val prefs = AppPreferences.getInstance(context)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            if (prefs.gmailBackupEmail.isNotBlank()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(prefs.gmailBackupEmail))
            }
            putExtra(Intent.EXTRA_SUBJECT, "My e-Khata Backup - ${FormatUtils.formatDateShort(System.currentTimeMillis())}")
            putExtra(Intent.EXTRA_TEXT, "Please find the e-Khata backup attached.\n\nGenerated by My e-Khata app.")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setPackage("com.google.android.gm")
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val chooser = Intent.createChooser(intent.apply {
                setPackage(null)
            }, "Send Backup via Email").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }
}
