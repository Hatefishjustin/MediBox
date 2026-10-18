package com.medibox.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.medibox.data.local.db.entity.Location
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Insert
    suspend fun insert(location: Location): Long

    @Update
    suspend fun update(location: Location)

    @Delete
    suspend fun delete(location: Location)

    @Query("SELECT * FROM locations ORDER BY sort_order ASC, id ASC")
    fun observeAll(): Flow<List<Location>>

    @Query("SELECT * FROM locations WHERE id = :id")
    suspend fun getById(id: Long): Location?

    @Query("SELECT * FROM locations WHERE is_default = 1 LIMIT 1")
    suspend fun getDefault(): Location?

    @Query("SELECT COUNT(*) FROM locations WHERE is_default = 1")
    suspend fun getDefaultCount(): Int

    @Query("SELECT COUNT(*) FROM locations")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM medicines WHERE location_id = :locationId")
    suspend fun medicineCountForLocation(locationId: Long): Int
}
