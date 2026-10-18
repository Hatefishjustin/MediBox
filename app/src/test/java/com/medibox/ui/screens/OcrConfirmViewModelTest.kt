package com.medibox.ui.screens

import com.medibox.TestMediBoxApplication
import com.medibox.data.local.db.MediBoxDatabase
import com.medibox.data.repository.LocationRepository
import com.medibox.data.repository.MedicineRepository
import com.medibox.domain.ApproxDate
import com.medibox.domain.ocr.OcrCandidate
import com.medibox.domain.ocr.OcrFieldCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.medibox.data.local.db.AppPhotoFileManager
import java.io.File
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestMediBoxApplication::class)
class OcrConfirmViewModelTest {

    @Test
    fun candidate_initializes_form_and_preserves_year_month_precision() = runWithMainTest {
        val env = TestEnv.create()
        val candidate = OcrCandidate(
            nameCandidates = listOf(OcrFieldCandidate("布洛芬缓释胶囊")),
            specificationCandidates = listOf(OcrFieldCandidate("0.3g*20片")),
            manufacturerCandidates = listOf(OcrFieldCandidate("山东某药业")),
            batchNoCandidates = listOf(OcrFieldCandidate("B2026-01")),
            productionDateCandidates = listOf(OcrFieldCandidate("2026-03")),
            expiryDateCandidates = listOf(OcrFieldCandidate("2028-03-15"))
        )
        val vm = OcrConfirmViewModel(env.medicineRepository, env.locationRepository, candidate)
        val state = vm.uiState.first { it is OcrConfirmUiState.Editing } as OcrConfirmUiState.Editing
        assertEquals("布洛芬缓释胶囊", state.form.name)
        assertEquals("0.3g*20片", state.form.specification)
        assertEquals("2026-03", state.form.productionDate)
        assertEquals("2028-03-15", state.form.expiryDate)
        assertEquals("我的药箱", state.form.locationName)
    }

    @Test
    fun empty_candidate_allows_manual_entry() = runWithMainTest {
        val env = TestEnv.create()
        val vm = OcrConfirmViewModel(env.medicineRepository, env.locationRepository, null)
        val state = vm.uiState.first { it is OcrConfirmUiState.Editing } as OcrConfirmUiState.Editing
        assertTrue(state.form.name.isEmpty())
        assertTrue(state.form.sourceCandidates == null)
        assertEquals("我的药箱", state.form.locationName)
    }

    @Test
    fun invalid_date_blocks_save_and_keeps_form() = runWithMainTest {
        val env = TestEnv.create()
        val vm = OcrConfirmViewModel(env.medicineRepository, env.locationRepository, null)
        vm.uiState.first { it is OcrConfirmUiState.Editing }

        vm.onNameChanged("测试药品")
        vm.onProductionDateChanged("2026-13")
        vm.save()

        val state = vm.uiState.value as OcrConfirmUiState.Error
        assertEquals("生产日期格式不正确", state.message)
        assertEquals("测试药品", state.form.name)
        assertEquals("2026-13", state.form.productionDate)
    }

    @Test
    fun save_success_returns_medicine_id_and_persists_year_month_precision() = runWithMainTest {
        val env = TestEnv.create()
        val vm = OcrConfirmViewModel(env.medicineRepository, env.locationRepository, null)
        vm.uiState.first { it is OcrConfirmUiState.Editing }

        vm.onNameChanged("测试药品")
        vm.onProductionDateChanged("2026-03")
        vm.onExpiryDateChanged("2028-03")
        vm.save()
        val success = vm.uiState.first { it is OcrConfirmUiState.Success } as OcrConfirmUiState.Success
        val saved = env.medicineRepository.getMedicine(success.medicineId)
        assertTrue(saved != null)
        assertEquals(ApproxDate.Precision.YEAR_MONTH, saved!!.productionDate?.precision)
        assertEquals(ApproxDate.Precision.YEAR_MONTH, saved.expiryDate?.precision)
        assertEquals(2026, saved.productionDate?.year)
        assertEquals(3, saved.productionDate?.month)
        assertEquals(2028, saved.expiryDate?.year)
        assertEquals(3, saved.expiryDate?.month)
    }

    @Test
    fun save_failure_keeps_form() = runWithMainTest {
        val env = TestEnv.create()
        val vm = OcrConfirmViewModel(env.medicineRepository, env.locationRepository, null)
        vm.uiState.first { it is OcrConfirmUiState.Editing }

        vm.onNameChanged("测试药品")
        vm.onProductionDateChanged("2026-13")
        vm.save()
        val state = vm.uiState.first { it is OcrConfirmUiState.Error } as OcrConfirmUiState.Error
        assertEquals("生产日期格式不正确", state.message)
        assertEquals("测试药品", state.form.name)
        assertEquals("2026-13", state.form.productionDate)
    }

    private fun runWithMainTest(block: suspend () -> Unit) = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class TestEnv private constructor(
        val db: MediBoxDatabase,
        val medicineRepository: MedicineRepository,
        val locationRepository: LocationRepository,
    ) {
        companion object {
            fun create(): TestEnv {
                val context = ApplicationProvider.getApplicationContext<android.content.Context>()
                val db = Room.inMemoryDatabaseBuilder(context, MediBoxDatabase::class.java)
                    .allowMainThreadQueries()
                    .build()
                val photoDir = File(context.cacheDir, "photos_test")
                val medicineRepo = MedicineRepository(db, db.medicineDao(), db.stockRecordDao(), AppPhotoFileManager(photoDir))
                val locationRepo = LocationRepository(db.locationDao())
                return TestEnv(db, medicineRepo, locationRepo)
            }
        }
    }
}
