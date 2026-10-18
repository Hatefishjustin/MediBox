package com.medibox.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.medibox.data.local.db.MediBoxDatabase
import com.medibox.data.local.db.PhotoFileManager
import com.medibox.data.local.db.entity.Medicine
import com.medibox.data.repository.LocationRepository
import com.medibox.data.repository.MedicineRepository
import com.medibox.domain.ApproxDate
import com.medibox.domain.StockChangeType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.medibox.TestMediBoxApplication

/**
 * S1 数据层测试（Robolectric，JVM 上跑 Room in-memory）。
 * 覆盖：Medicine/Location CRUD、StockRecord 查询、6 种 StockChangeType、库存不为负、
 * 事务回滚、ApproxDate 两种精度持久化、默认 Location 只创建一次、删除药品级联流水、照片文件删除。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestMediBoxApplication::class)
class MediBoxDataLayerTest {

    private lateinit var db: MediBoxDatabase
    private lateinit var medicineRepo: MedicineRepository
    private lateinit var locationRepo: LocationRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MediBoxDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        medicineRepo = MedicineRepository(
            db = db,
            medicineDao = db.medicineDao(),
            stockRecordDao = db.stockRecordDao(),
            photoFileManager = NoOpPhotoFileManager
        )
        locationRepo = LocationRepository(db.locationDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ---------- CRUD ----------

    @Test
    fun medicineCrud() = runBlocking {
        val id = medicineRepo.insertMedicine(Medicine(name = "布洛芬", stock = 1))
        val m = medicineRepo.getMedicine(id)
        assertNotNull(m)
        assertEquals("布洛芬", m!!.name)
        medicineRepo.updateMedicine(m.copy(stock = 3, manufacturer = "某厂"))
        val updated = medicineRepo.getMedicine(id)!!
        assertEquals(3, updated.stock)
        assertEquals("某厂", updated.manufacturer)
        medicineRepo.deleteMedicine(updated)
        assertNull(medicineRepo.getMedicine(id))
    }

    @Test
    fun locationCrud() = runBlocking {
        val id = locationRepo.insert("客厅抽屉")
        val loc = locationRepo.getLocation(id)
        assertNotNull(loc)
        assertEquals("客厅抽屉", loc!!.name)
        locationRepo.update(loc.copy(sortOrder = 5))
        assertEquals(5, locationRepo.getLocation(id)!!.sortOrder)
        locationRepo.delete(locationRepo.getLocation(id)!!)
        assertNull(locationRepo.getLocation(id))
    }

    @Test
    fun stockRecordQuery() = runBlocking {
        val id = medicineRepo.insertMedicine(Medicine(name = "A", stock = 10))
        medicineRepo.purchaseStock(id, 5)  // 15
        medicineRepo.useStock(id, 2)        // 13
        val records = medicineRepo.getStockRecords(id)
        assertEquals(2, records.size)
        assertEquals(13, medicineRepo.getMedicine(id)!!.stock)
    }

    // ---------- 6 种 StockChangeType ----------

    @Test
    fun purchaseIncreases() = runBlocking {
        val id = medicineRepo.insertMedicine(Medicine(name = "A", stock = 10))
        medicineRepo.purchaseStock(id, 5)
        assertEquals(15, medicineRepo.getMedicine(id)!!.stock)
        val r = medicineRepo.getStockRecords(id).last()
        assertEquals(StockChangeType.PURCHASE, r.changeType)
        assertEquals(5, r.amount)
    }

    @Test
    fun increaseIncreases() = runBlocking {
        val id = medicineRepo.insertMedicine(Medicine(name = "A", stock = 8))
        medicineRepo.addStock(id, 4)
        assertEquals(12, medicineRepo.getMedicine(id)!!.stock)
        assertEquals(StockChangeType.INCREASE, medicineRepo.getStockRecords(id).last().changeType)
    }

    @Test
    fun useDecreases() = runBlocking {
        val id = medicineRepo.insertMedicine(Medicine(name = "A", stock = 10))
        medicineRepo.useStock(id, 2)
        assertEquals(8, medicineRepo.getMedicine(id)!!.stock)
        assertEquals(StockChangeType.USE, medicineRepo.getStockRecords(id).last().changeType)
    }

    @Test
    fun decreaseDecreases() = runBlocking {
        val id = medicineRepo.insertMedicine(Medicine(name = "A", stock = 10))
        medicineRepo.decreaseStock(id, 3)
        assertEquals(7, medicineRepo.getMedicine(id)!!.stock)
        assertEquals(StockChangeType.DECREASE, medicineRepo.getStockRecords(id).last().changeType)
    }

    @Test
    fun discardDecreases() = runBlocking {
        val id = medicineRepo.insertMedicine(Medicine(name = "A", stock = 10))
        medicineRepo.discardStock(id, 1)
        assertEquals(9, medicineRepo.getMedicine(id)!!.stock)
        assertEquals(StockChangeType.DISCARD, medicineRepo.getStockRecords(id).last().changeType)
    }

    @Test
    fun adjustSetsAbsolute() = runBlocking {
        val id = medicineRepo.insertMedicine(Medicine(name = "A", stock = 13))
        medicineRepo.adjustStock(id, 20)
        assertEquals(20, medicineRepo.getMedicine(id)!!.stock)
        val r = medicineRepo.getStockRecords(id).last()
        assertEquals(StockChangeType.ADJUST, r.changeType)
        assertEquals(20, r.amount)   // amount = 最终库存
        assertEquals(20, r.balance)
    }

    // ---------- 库存不为负 + 事务回滚 ----------

    @Test
    fun stockNeverNegative() = runBlocking {
        val id = medicineRepo.insertMedicine(Medicine(name = "A", stock = 5))
        var threw = false
        try {
            medicineRepo.useStock(id, 10)  // 5 - 10 = -5 -> 拒绝
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
        // 库存未变、无流水（事务回滚，未留下半截状态）
        assertEquals(5, medicineRepo.getMedicine(id)!!.stock)
        assertEquals(0, medicineRepo.getStockRecords(id).size)
    }

    @Test
    fun transactionRollback() = runBlocking {
        val id = medicineRepo.insertMedicine(Medicine(name = "A", stock = 5))
        try {
            medicineRepo.discardStock(id, 100)  // 必失败
        } catch (_: IllegalArgumentException) {
            // expected
        }
        assertEquals(5, medicineRepo.getMedicine(id)!!.stock)
        assertEquals(0, db.stockRecordDao().countForMedicine(id))
    }

    @Test
    fun amountMustBePositive() = runBlocking {
        val id = medicineRepo.insertMedicine(Medicine(name = "A", stock = 5))
        var threw = false
        try {
            medicineRepo.useStock(id, 0)  // 非正
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
        assertEquals(5, medicineRepo.getMedicine(id)!!.stock)
    }

    // ---------- ApproxDate 持久化（精度安全）----------

    @Test
    fun approxDateYearMonthPersisted() = runBlocking {
        val exp = ApproxDate.parse("2028-03")
        val id = medicineRepo.insertMedicine(Medicine(name = "A", expiryDate = exp))
        val persisted = medicineRepo.getMedicine(id)!!.expiryDate!!
        assertEquals(ApproxDate.Precision.YEAR_MONTH, persisted.precision)
        assertEquals("2028-03", persisted.toStorageString())
        // 绝不被偷偷补成完整日
        assertFalse(
            "YEAR_MONTH 不应被转成 FULL_DATE",
            persisted.precision == ApproxDate.Precision.FULL_DATE
        )
    }

    @Test
    fun approxDateFullPersisted() = runBlocking {
        val exp = ApproxDate.parse("2028-03-15")
        val id = medicineRepo.insertMedicine(Medicine(name = "A", expiryDate = exp))
        val persisted = medicineRepo.getMedicine(id)!!.expiryDate!!
        assertEquals(ApproxDate.Precision.FULL_DATE, persisted.precision)
        assertEquals(15, persisted.day)
        assertEquals("2028-03-15", persisted.toStorageString())
    }

    // ---------- 默认 Location 只创建一次 ----------

    @Test
    fun defaultLocationCreatedOnce() = runBlocking {
        locationRepo.ensureDefaultLocation()
        locationRepo.ensureDefaultLocation()
        locationRepo.ensureDefaultLocation()
        assertEquals(1, db.locationDao().getDefaultCount())
        val def = db.locationDao().getDefault()
        assertNotNull(def)
        assertEquals("我的药箱", def!!.name)
        assertTrue(def.isDefault)
    }

    @Test
    fun defaultLocationNotDeletable() = runBlocking {
        locationRepo.ensureDefaultLocation()
        val def = locationRepo.getDefault()!!
        var threw = false
        try {
            locationRepo.delete(def)
        } catch (_: IllegalStateException) {
            threw = true
        }
        assertTrue(threw)
        assertNotNull(locationRepo.getDefault())
    }

    @Test
    fun locationWithMedicinesNotDeletable() = runBlocking {
        val locId = locationRepo.insert("客厅")
        medicineRepo.insertMedicine(Medicine(name = "A", locationId = locId))
        val loc = locationRepo.getLocation(locId)!!
        var threw = false
        try {
            locationRepo.delete(loc)
        } catch (_: IllegalStateException) {
            threw = true
        }
        assertTrue(threw)
    }

    // ---------- 删除药品级联 StockRecord ----------

    @Test
    fun deleteMedicineCascadesStockRecords() = runBlocking {
        val id = medicineRepo.insertMedicine(Medicine(name = "A", stock = 5))
        medicineRepo.purchaseStock(id, 3)  // 产生一条流水
        assertEquals(1, db.stockRecordDao().countForMedicine(id))
        medicineRepo.deleteMedicine(medicineRepo.getMedicine(id)!!)
        assertNull(medicineRepo.getMedicine(id))
        assertEquals(0, db.stockRecordDao().countForMedicine(id))  // 级联删除
    }

    // ---------- 照片文件删除 ----------

    @Test
    fun deleteMedicineDeletesPhotoFile() = runBlocking {
        val rec = RecordingPhotoFileManager()
        val repo = MedicineRepository(db, db.medicineDao(), db.stockRecordDao(), rec)
        val id = repo.insertMedicine(Medicine(name = "A", photoPath = "/x/photo.jpg"))
        repo.deleteMedicine(repo.getMedicine(id)!!)
        assertEquals(listOf("/x/photo.jpg"), rec.deleted)
    }

    @Test
    fun replacePhotoDeletesOldFile() = runBlocking {
        val rec = RecordingPhotoFileManager()
        val repo = MedicineRepository(db, db.medicineDao(), db.stockRecordDao(), rec)
        val id = repo.insertMedicine(Medicine(name = "A", photoPath = "/x/old.jpg"))
        repo.setMedicinePhoto(id, "/x/new.jpg")
        assertEquals(listOf("/x/old.jpg"), rec.deleted)
    }

    // ---- 假实现 ----

    private object NoOpPhotoFileManager : PhotoFileManager {
        override fun deleteIfExists(path: String?): Boolean = false
        override fun deleteOldIfDifferent(oldPath: String?, newPath: String?) {}
    }

    private class RecordingPhotoFileManager : PhotoFileManager {
        val deleted = mutableListOf<String?>()
        override fun deleteIfExists(path: String?): Boolean {
            deleted.add(path)
            return true
        }
        override fun deleteOldIfDifferent(oldPath: String?, newPath: String?) {
            if (oldPath != null && oldPath != newPath) deleteIfExists(oldPath)
        }
    }
}
