package com.medibox

import android.app.Application
import com.medibox.data.local.db.AppPhotoFileManager
import com.medibox.data.local.db.MediBoxDatabase
import com.medibox.data.repository.LocationRepository
import com.medibox.data.repository.MedicineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class MediBoxApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { MediBoxDatabase.build(this) }

    val medicineRepository by lazy {
        MedicineRepository(
            db = database,
            medicineDao = database.medicineDao(),
            stockRecordDao = database.stockRecordDao(),
            photoFileManager = AppPhotoFileManager(File(getExternalFilesDir(null), "photos"))
        )
    }

    val locationRepository by lazy {
        LocationRepository(database.locationDao())
    }

    override fun onCreate() {
        super.onCreate()
        // 首次启动播种默认位置「我的药箱」（幂等，只创建一次）
        applicationScope.launch {
            locationRepository.ensureDefaultLocation()
        }
    }
}
