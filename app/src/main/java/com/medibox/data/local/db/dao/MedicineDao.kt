package com.medibox.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.medibox.data.local.db.entity.Medicine
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Insert
    suspend fun insert(medicine: Medicine): Long

    @Update
    suspend fun update(medicine: Medicine)

    @Delete
    suspend fun delete(medicine: Medicine)

    @Query("UPDATE medicines SET stock = :stock, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateStock(id: Long, stock: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getById(id: Long): Medicine?

    @Query("SELECT * FROM medicines ORDER BY created_at DESC")
    fun observeAll(): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE id = :id")
    fun observeById(id: Long): Flow<Medicine?>

    @Query(
        """
        SELECT * FROM medicines
        WHERE name LIKE '%' || :query || '%'
           OR specification LIKE '%' || :query || '%'
           OR manufacturer LIKE '%' || :query || '%'
           OR batch_no LIKE '%' || :query || '%'
        ORDER BY name ASC
        """
    )
    fun search(query: String): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE location_id = :locationId ORDER BY name ASC")
    fun getByLocation(locationId: Long): Flow<List<Medicine>>

    @Query("SELECT COUNT(*) FROM medicines")
    suspend fun count(): Int
}
