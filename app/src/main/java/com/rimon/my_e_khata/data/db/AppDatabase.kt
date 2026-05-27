package com.rimon.my_e_khata.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.rimon.my_e_khata.data.model.CashbookEntry
import com.rimon.my_e_khata.data.model.Customer
import com.rimon.my_e_khata.data.model.SmsLog
import com.rimon.my_e_khata.data.model.Supplier
import com.rimon.my_e_khata.data.model.Transaction

@Database(
    entities = [Customer::class, Supplier::class, Transaction::class, CashbookEntry::class, SmsLog::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun supplierDao(): SupplierDao
    abstract fun transactionDao(): TransactionDao
    abstract fun cashbookDao(): CashbookDao
    abstract fun smsLogDao(): SmsLogDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "my_ekhata_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
