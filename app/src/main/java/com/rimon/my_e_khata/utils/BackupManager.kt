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

        // Fetch all data synchronously on IO dispatcher
        val customers = db.openHelper.readableDatabase.let { raw ->
            val list = mutableListOf<String>()
            val c = raw.rawQuery("SELECT id,name,mobile,email,address,balance,autoSmsEnabled FROM customers ORDER BY id", null)
            while (c.moveToNext()) {
                list.add("${c.getLong(0)},\"${c.getString(1)}\",${c.getString(2)},${c.getString(3)},\"${c.getString(4)}\",${c.getDouble(5)},${c.getInt(6)}")
            }
            c.close()
            list
        }

        val suppliers = db.openHelper.readableDatabase.let { raw ->
            val list = mutableListOf<String>()
            val c = raw.rawQuery("SELECT id,name,mobile,email,address,balance,autoSmsEnabled FROM suppliers ORDER BY id", null)
            while (c.moveToNext()) {
                list.add("${c.getLong(0)},\"${c.getString(1)}\",${c.getString(2)},${c.getString(3)},\"${c.getString(4)}\",${c.getDouble(5)},${c.getInt(6)}")
            }
            c.close()
            list
        }

        val transactions = db.openHelper.readableDatabase.let { raw ->
            val list = mutableListOf<String>()
            val c = raw.rawQuery("SELECT id,partyId,partyType,type,amount,balance,note,createdAt FROM transactions ORDER BY createdAt", null)
            while (c.moveToNext()) {
                list.add("${c.getLong(0)},${c.getLong(1)},${c.getString(2)},${c.getString(3)},${c.getDouble(4)},${c.getDouble(5)},\"${c.getString(6)}\",${c.getLong(7)}")
            }
            c.close()
            list
        }

        val cashbook = db.openHelper.readableDatabase.let { raw ->
            val list = mutableListOf<String>()
            val c = raw.rawQuery("SELECT id,type,amount,balance,category,note,createdAt FROM cashbook ORDER BY createdAt", null)
            while (c.moveToNext()) {
                list.add("${c.getLong(0)},${c.getString(1)},${c.getDouble(2)},${c.getDouble(3)},\"${c.getString(4)}\",\"${c.getString(5)}\",${c.getLong(6)}")
            }
            c.close()
            list
        }

        FileWriter(file).use { w ->
            w.write("=== MY E-KHATA BACKUP ===\n")
            w.write("Generated: ${FormatUtils.formatDateShort(System.currentTimeMillis())}\n\n")

            w.write("CUSTOMERS\n")
            w.write("ID,Name,Mobile,Email,Address,Balance,AutoSMS\n")
            customers.forEach { row -> w.write("$row\n") }

            w.write("\nSUPPLIERS\n")
            w.write("ID,Name,Mobile,Email,Address,Balance,AutoSMS\n")
            suppliers.forEach { row -> w.write("$row\n") }

            w.write("\nTRANSACTIONS\n")
            w.write("ID,PartyID,PartyType,Type,Amount,Balance,Note,CreatedAt\n")
            transactions.forEach { row -> w.write("$row\n") }

            w.write("\nCASHBOOK\n")
            w.write("ID,Type,Amount,Balance,Category,Note,CreatedAt\n")
            cashbook.forEach { row -> w.write("$row\n") }
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
            putExtra(Intent.EXTRA_TEXT, "My e-Khata backup attached.")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setPackage("com.google.android.gm")
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val chooser = Intent.createChooser(
                intent.apply { setPackage(null) },
                "Send Backup via Email"
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(chooser)
        }
    }
}
