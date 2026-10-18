package com.medibox.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.medibox.data.local.db.dao.LocationDao
import com.medibox.data.local.db.dao.MedicineDao
import com.medibox.data.local.db.dao.StockRecordDao
import com.medibox.data.local.db.entity.Location
import com.medibox.data.local.db.entity.Medicine
import com.medibox.data.local.db.entity.StockRecord

@Database(
    entities = [Medicine::class, Location::class, StockRecord::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MediBoxDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
    abstract fun locationDao(): LocationDao
    abstract fun stockRecordDao(): StockRecordDao

    companion object {
        const val DATABASE_NAME = "medibox.db"

        fun build(context: Context): MediBoxDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                MediBoxDatabase::class.java,
                DATABASE_NAME
            ).fallbackToDestructiveMigration().build()
    }
}
