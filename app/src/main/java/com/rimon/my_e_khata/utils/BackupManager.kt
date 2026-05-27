package com.rimon.my_e_khata.utils

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rimon.my_e_khata.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object BackupManager {

    private fun queryRows(db: SupportSQLiteDatabase, sql: String): List<String> {
        val list = mutableListOf<String>()
        val cursor = db.query(sql)
        try {
            while (cursor.moveToNext()) {
                val cols = (0 until cursor.columnCount).map { i ->
                    when (cursor.getType(i)) {
                        android.database.Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i).toString()
                        android.database.Cursor.FIELD_TYPE_FLOAT   -> cursor.getDouble(i).toString()
                        else -> "\"${cursor.getString(i) ?: ""}\""
                    }
                }
                list.add(cols.joinToString(","))
            }
        } finally {
            cursor.close()
        }
        return list
    }

    suspend fun generateBackupCsv(context: Context): File = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context).openHelper.readableDatabase
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val file = File(dir, "ekhata_backup_$timestamp.csv")

        val customers    = queryRows(db, "SELECT id,name,mobile,email,address,balance,autoSmsEnabled FROM customers ORDER BY id")
        val suppliers    = queryRows(db, "SELECT id,name,mobile,email,address,balance,autoSmsEnabled FROM suppliers ORDER BY id")
        val transactions = queryRows(db, "SELECT id,partyId,partyType,type,amount,balance,note,createdAt FROM transactions ORDER BY createdAt")
        val cashbook     = queryRows(db, "SELECT id,type,amount,balance,category,note,createdAt FROM cashbook ORDER BY createdAt")

        FileWriter(file).use { w ->
            w.write("=== MY E-KHATA BACKUP ===\n")
            w.write("Generated: ${FormatUtils.formatDateShort(System.currentTimeMillis())}\n\n")

            w.write("CUSTOMERS\n")
            w.write("ID,Name,Mobile,Email,Address,Balance,AutoSMS\n")
            customers.forEach { w.write("$it\n") }

            w.write("\nSUPPLIERS\n")
            w.write("ID,Name,Mobile,Email,Address,Balance,AutoSMS\n")
            suppliers.forEach { w.write("$it\n") }

            w.write("\nTRANSACTIONS\n")
            w.write("ID,PartyID,PartyType,Type,Amount,Balance,Note,CreatedAt\n")
            transactions.forEach { w.write("$it\n") }

            w.write("\nCASHBOOK\n")
            w.write("ID,Type,Amount,Balance,Category,Note,CreatedAt\n")
            cashbook.forEach { w.write("$it\n") }
        }

        AppPreferences.getInstance(context).lastBackupTime = System.currentTimeMillis()
        file
    }

    suspend fun shareBackupViaGmail(context: Context) {
        val file = generateBackupCsv(context)
        val prefs = AppPreferences.getInstance(context)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

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
            val chooser = Intent.createChooser(intent.apply { setPackage(null) }, "Send Backup via Email")
                .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(chooser)
        }
    }
}
