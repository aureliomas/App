package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.OpticaDao
import com.example.data.model.*

@Database(
    entities = [
        Patient::class,
        ClinicalRecord::class,
        Appointment::class,
        InventoryItem::class,
        Invoice::class,
        CashCut::class,
        UserAccount::class,
        FinancialRecord::class
    ],
    version = 6,
    exportSchema = false
)
abstract class OpticaDatabase : RoomDatabase() {
    abstract fun opticaDao(): OpticaDao

    companion object {
        @Volatile
        private var INSTANCE: OpticaDatabase? = null

        fun getDatabase(context: Context): OpticaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OpticaDatabase::class.java,
                    "optica_care_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
