package com.medibox.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.medibox.data.local.db.entity.StockRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface StockRecordDao {
    @Insert
    suspend fun insert(record: StockRecord): Long

    @Query("SELECT * FROM stock_records WHERE medicine_id = :medicineId ORDER BY created_at ASC, id ASC")
    fun observeForMedicine(medicineId: Long): Flow<List<StockRecord>>

    @Query("SELECT * FROM stock_records WHERE medicine_id = :medicineId ORDER BY created_at ASC, id ASC")
    suspend fun getForMedicine(medicineId: Long): List<StockRecord>

    @Query("SELECT COUNT(*) FROM stock_records")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM stock_records WHERE medicine_id = :medicineId")
    suspend fun countForMedicine(medicineId: Long): Int
}
