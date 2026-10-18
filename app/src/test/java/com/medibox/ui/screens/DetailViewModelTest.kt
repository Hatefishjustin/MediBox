package com.medibox.ui.screens

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.medibox.TestMediBoxApplication
import com.medibox.data.local.db.MediBoxDatabase
import com.medibox.data.local.db.PhotoFileManager
import com.medibox.data.local.db.entity.Medicine
import com.medibox.data.repository.LocationRepository
import com.medibox.data.repository.MedicineRepository
import com.medibox.domain.ApproxDate
import com.medibox.domain.ExpiryStatus
import com.medibox.domain.StockChangeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestMediBoxApplication::class)
class DetailViewModelTest {

    private lateinit var db: MediBoxDatabase
    private lateinit var medicineRepository: MedicineRepository
    private lateinit var locationRepository: LocationRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MediBoxDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        medicineRepository = MedicineRepository(
            db = db,
            medicineDao = db.medicineDao(),
            stockRecordDao = db.stockRecordDao(),
            photoFileManager = NoOpPhotoFileManager
        )
        locationRepository = LocationRepository(db.locationDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun loads_medicine_location_and_expiry_status() = runWithMainTest {
        val locationId = locationRepository.insert("客厅药箱")
        val medicineId = medicineRepository.insertMedicine(
            Medicine(
                name = "测试药品",
                specification = "0.25g*24粒",
                manufacturer = "某药业",
                batchNo = "B-001",
                productionDate = ApproxDate.parse("2026-03"),
                expiryDate = ApproxDate.parse("2028-03-15"),
                locationId = locationId,
                stock = 3,
                usageNote = "用户保存的信息",
            )
        )
        val vm = DetailViewModel(medicineId, medicineRepository, locationRepository)
        val state = vm.uiState.first { !it.loading }
        assertEquals(false, state.loading)
        assertEquals("测试药品", state.medicine?.name)
        assertEquals("客厅药箱", state.location?.name)
        assertEquals(ExpiryStatus.NORMAL, state.expiryStatus)
        assertEquals("2028年3月15日", state.expiryLabel)
    }

    @Test
    fun missing_medicine_returns_error() = runWithMainTest {
        val vm = DetailViewModel(999L, medicineRepository, locationRepository)
        val state = vm.uiState.first { !it.loading }
        assertTrue(state.medicine == null)
        assertEquals("药品不存在或已被删除", state.errorMessage)
    }

    @Test
    fun editing_form_initializes_from_medicine() = runWithMainTest {
        val locationId = locationRepository.insert("卧室药箱")
        val medicineId = medicineRepository.insertMedicine(
            Medicine(
                name = "编辑药品",
                specification = "0.5g",
                manufacturer = "厂商A",
                batchNo = "B-888",
                productionDate = ApproxDate.parse("2026-03"),
                expiryDate = ApproxDate.parse("2028-03"),
                locationId = locationId,
                stock = 2,
                usageNote = "用法",
                contraindicationNote = "禁忌",
                precautionNote = "注意事项",
                userNote = "备注",
            )
        )
        val vm = DetailViewModel(medicineId, medicineRepository, locationRepository)
        vm.uiState.first { !it.loading }

        vm.enterEditMode()
        val form = vm.uiState.value.editingForm

        assertEquals("编辑药品", form?.name)
        assertEquals("0.5g", form?.specification)
        assertEquals("厂商A", form?.manufacturer)
        assertEquals("B-888", form?.batchNo)
        assertEquals("2026-03", form?.productionDate)
        assertEquals("2028-03", form?.expiryDate)
        assertEquals(locationId, form?.locationId)
        assertEquals("卧室药箱", form?.locationName)
    }

    @Test
    fun stock_operation_updates_repository() = runWithMainTest {
        val medicineId = medicineRepository.insertMedicine(Medicine(name = "测试药品", stock = 2))
        val vm = DetailViewModel(medicineId, medicineRepository, locationRepository)
        vm.uiState.first { !it.loading }

        vm.submitStockOperation(StockOperationType.INCREASE, "2")
        val state = vm.uiState.first { !it.loading && it.medicine?.stock == 4 }

        val saved = medicineRepository.getMedicine(medicineId)
        assertEquals(4, saved?.stock)
        assertEquals(4, state.medicine?.stock)
        val lastRecord = medicineRepository.getStockRecords(medicineId).lastOrNull()
        assertEquals(StockChangeType.INCREASE, lastRecord?.changeType)
        assertEquals(2, lastRecord?.amount)
    }

    @Test
    fun invalid_stock_operation_keeps_error() = runWithMainTest {
        val medicineId = medicineRepository.insertMedicine(Medicine(name = "测试药品", stock = 2))
        val vm = DetailViewModel(medicineId, medicineRepository, locationRepository)
        vm.uiState.first { !it.loading }

        vm.submitStockOperation(StockOperationType.USE, "0")

        val state = vm.uiState.value
        assertEquals("数量必须是正整数", state.errorMessage)
        assertEquals(2, medicineRepository.getMedicine(medicineId)?.stock)
    }

    @Test
    fun delete_medicine_removes_entity_and_records() = runWithMainTest {
        val medicineId = medicineRepository.insertMedicine(Medicine(name = "测试药品", stock = 2))
        medicineRepository.purchaseStock(medicineId, 1)
        val vm = DetailViewModel(medicineId, medicineRepository, locationRepository)
        vm.uiState.first { !it.loading }

        vm.deleteMedicine()
        vm.uiState.first { it.successMessage == "药品已删除" }

        assertNull(medicineRepository.getMedicine(medicineId))
        assertEquals(0, medicineRepository.getStockRecords(medicineId).size)
        assertEquals("药品已删除", vm.uiState.value.successMessage)
    }

    @Test
    fun editing_invalid_date_is_rejected() = runWithMainTest {
        val medicineId = medicineRepository.insertMedicine(Medicine(name = "测试药品", stock = 2))
        val vm = DetailViewModel(medicineId, medicineRepository, locationRepository)
        vm.uiState.first { !it.loading }

        vm.enterEditMode()
        vm.updateEditForm { copy(productionDate = "2026-13") }
        vm.saveEdit()

        val state = vm.uiState.value
        assertEquals("生产日期格式不正确", state.errorMessage)
        assertNotNull(state.editingForm)
    }

    @Test
    fun editing_preserves_year_month_precision() = runWithMainTest {
        val medicineId = medicineRepository.insertMedicine(Medicine(name = "测试药品", stock = 2))
        val vm = DetailViewModel(medicineId, medicineRepository, locationRepository)
        vm.uiState.first { !it.loading }

        vm.enterEditMode()
        vm.updateEditForm {
            copy(
                productionDate = "2026-03",
                expiryDate = "2028-03",
            )
        }
        vm.saveEdit()
        vm.uiState.first { it.loading.not() && it.medicine != null && it.editingForm == null }

        val saved = medicineRepository.getMedicine(medicineId)
        assertEquals(ApproxDate.Precision.YEAR_MONTH, saved?.productionDate?.precision)
        assertEquals(ApproxDate.Precision.YEAR_MONTH, saved?.expiryDate?.precision)
    }

    private object NoOpPhotoFileManager : PhotoFileManager {
        override fun deleteIfExists(path: String?) = true
        override fun deleteOldIfDifferent(oldPath: String?, newPath: String?) = Unit
    }

    private fun runWithMainTest(block: suspend () -> Unit) = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }
}
