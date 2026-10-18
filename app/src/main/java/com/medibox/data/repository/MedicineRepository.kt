package com.medibox.data.repository

import androidx.room.withTransaction
import com.medibox.data.local.db.MediBoxDatabase
import com.medibox.data.local.db.PhotoFileManager
import com.medibox.data.local.db.dao.MedicineDao
import com.medibox.data.local.db.dao.StockRecordDao
import com.medibox.data.local.db.entity.Medicine
import com.medibox.data.local.db.entity.StockRecord
import com.medibox.domain.StockChangeType
import kotlinx.coroutines.flow.Flow

/**
 * 药品仓库：CRUD + 库存变更（统一走事务）。
 *
 * 库存铁律：
 * - [Medicine.stock] 是当前库存的**唯一真值**；[StockRecord.balance] 只是历史快照。
 * - 任何库存修改都通过下方事务方法完成，内部步骤（改 stock + 写流水）要么全成功、要么全回滚，
 *   绝不会出现「只改了 stock 没流水」或「只写了流水没改 stock」。
 * - UI 不应直接调用 DAO 改库存，必须使用 addStock/useStock/discardStock/adjustStock/purchaseStock。
 */
class MedicineRepository(
    private val db: MediBoxDatabase,
    private val medicineDao: MedicineDao,
    private val stockRecordDao: StockRecordDao,
    private val photoFileManager: PhotoFileManager
) {
    // ---------- CRUD ----------
    suspend fun insertMedicine(medicine: Medicine): Long = medicineDao.insert(medicine)
    suspend fun updateMedicine(medicine: Medicine) = medicineDao.update(medicine)
    suspend fun getMedicine(id: Long): Medicine? = medicineDao.getById(id)
    fun observeMedicines(): Flow<List<Medicine>> = medicineDao.observeAll()
    fun observeMedicine(id: Long): Flow<Medicine?> = medicineDao.observeById(id)
    fun searchMedicines(query: String): Flow<List<Medicine>> = medicineDao.search(query)
    fun getMedicinesByLocation(locationId: Long): Flow<List<Medicine>> =
        medicineDao.getByLocation(locationId)

    /** 删除药品：级联删除 StockRecord（Room 外键 CASCADE）+ 删除照片文件。 */
    suspend fun deleteMedicine(medicine: Medicine) {
        photoFileManager.deleteIfExists(medicine.photoPath)
        medicineDao.delete(medicine)
    }

    /** 替换/清空照片路径，并删除旧照片文件（若不同）。 */
    suspend fun setMedicinePhoto(medicineId: Long, newPhotoPath: String?) {
        val medicine = medicineDao.getById(medicineId) ?: return
        val old = medicine.photoPath
        medicineDao.update(medicine.copy(photoPath = newPhotoPath, updatedAt = System.currentTimeMillis()))
        photoFileManager.deleteOldIfDifferent(old, newPhotoPath)
    }

    // ---------- 库存操作（全部事务）----------

    /** 购入，增加库存。 */
    suspend fun purchaseStock(medicineId: Long, amount: Int, note: String? = null) =
        applyChange(medicineId, amount, StockChangeType.PURCHASE, note)

    /** 增加库存（其他来源）。 */
    suspend fun addStock(medicineId: Long, amount: Int, note: String? = null) =
        applyChange(medicineId, amount, StockChangeType.INCREASE, note)

    /** 使用，减少库存。 */
    suspend fun useStock(medicineId: Long, amount: Int, note: String? = null) =
        applyChange(medicineId, amount, StockChangeType.USE, note)

    /** 丢弃，减少库存。 */
    suspend fun discardStock(medicineId: Long, amount: Int, note: String? = null) =
        applyChange(medicineId, amount, StockChangeType.DISCARD, note)

    /** 减少（其他原因），减少库存。 */
    suspend fun decreaseStock(medicineId: Long, amount: Int, note: String? = null) =
        applyChange(medicineId, amount, StockChangeType.DECREASE, note)

    /** 直接设置最终库存（amount = 最终库存，必须 >= 0）。 */
    suspend fun adjustStock(medicineId: Long, newStock: Int, note: String? = null) {
        require(newStock >= 0) { "库存不能为负数" }
        db.withTransaction {
            val medicine = medicineDao.getById(medicineId)
                ?: throw IllegalArgumentException("药品不存在: $medicineId")
            medicineDao.updateStock(medicineId, newStock)
            stockRecordDao.insert(
                StockRecord(
                    medicineId = medicineId,
                    changeType = StockChangeType.ADJUST,
                    amount = newStock,
                    balance = newStock,
                    note = note
                )
            )
        }
    }

    private suspend fun applyChange(
        medicineId: Long,
        amount: Int,
        type: StockChangeType,
        note: String?
    ) {
        require(amount > 0) { "变更数量必须为正整数" }
        db.withTransaction {
            val medicine = medicineDao.getById(medicineId)
                ?: throw IllegalArgumentException("药品不存在: $medicineId")
            val current = medicine.stock
            val newBalance = when (type) {
                StockChangeType.PURCHASE, StockChangeType.INCREASE -> current + amount
                StockChangeType.USE, StockChangeType.DECREASE, StockChangeType.DISCARD -> current - amount
                StockChangeType.ADJUST -> amount // 不会在此分支命中
            }
            require(newBalance >= 0) { "库存不能为负数（当前 $current，变更 $amount，类型 $type）" }
            medicineDao.updateStock(medicineId, newBalance)
            stockRecordDao.insert(
                StockRecord(
                    medicineId = medicineId,
                    changeType = type,
                    amount = amount,
                    balance = newBalance,
                    note = note
                )
            )
        }
    }

    // ---------- 库存流水查询 ----------
    fun observeStockRecords(medicineId: Long): Flow<List<StockRecord>> =
        stockRecordDao.observeForMedicine(medicineId)

    suspend fun getStockRecords(medicineId: Long): List<StockRecord> =
        stockRecordDao.getForMedicine(medicineId)
}
