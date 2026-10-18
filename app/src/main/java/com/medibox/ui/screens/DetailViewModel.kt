package com.medibox.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medibox.data.local.db.entity.Location
import com.medibox.data.local.db.entity.Medicine
import com.medibox.data.local.db.entity.StockRecord
import com.medibox.data.repository.LocationRepository
import com.medibox.data.repository.MedicineRepository
import com.medibox.domain.ApproxDate
import com.medibox.domain.ExpiryChecker
import com.medibox.domain.ExpiryStatus
import com.medibox.domain.ocr.OcrCandidate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class DetailUiState(
    val loading: Boolean = true,
    val medicine: Medicine? = null,
    val location: Location? = null,
    val expiryStatus: ExpiryStatus = ExpiryStatus.UNKNOWN,
    val expiryLabel: String? = null,
    val stockRecords: List<StockRecord> = emptyList(),
    val editingForm: DetailEditFormState? = null,
    val saving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val operationMessage: String? = null,
)

data class DetailEditFormState(
    val name: String = "",
    val specification: String = "",
    val manufacturer: String = "",
    val batchNo: String = "",
    val productionDate: String = "",
    val expiryDate: String = "",
    val locationId: Long? = null,
    val locationName: String = "",
    val usageNote: String = "",
    val contraindicationNote: String = "",
    val precautionNote: String = "",
    val userNote: String = "",
    val photoPath: String? = null,
)

enum class StockOperationType {
    PURCHASE,
    INCREASE,
    USE,
    DISCARD,
    DECREASE,
    ADJUST,
}

data class StockOperationFormState(
    val type: StockOperationType,
    val amount: String = "",
    val note: String = "",
    val errorMessage: String? = null,
    val saving: Boolean = false,
)

class DetailViewModel(
    private val medicineId: Long,
    private val medicineRepository: MedicineRepository,
    private val locationRepository: LocationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            _uiState.value = DetailUiState(loading = true)
            val medicine = medicineRepository.getMedicine(medicineId)
            if (medicine == null) {
                _uiState.value = DetailUiState(
                    loading = false,
                    errorMessage = "药品不存在或已被删除"
                )
                return@launch
            }
            val location = medicine.locationId?.let { locationRepository.getLocation(it) }
            val stockRecords = medicineRepository.getStockRecords(medicineId)
            _uiState.value = buildState(medicine, location, stockRecords)
        }
    }

    fun enterEditMode() {
        val current = _uiState.value.medicine ?: return
        val location = _uiState.value.location
        _uiState.value = _uiState.value.copy(
            editingForm = DetailEditFormState(
                name = current.name,
                specification = current.specification.orEmpty(),
                manufacturer = current.manufacturer.orEmpty(),
                batchNo = current.batchNo.orEmpty(),
                productionDate = current.productionDate?.toStorageString().orEmpty(),
                expiryDate = current.expiryDate?.toStorageString().orEmpty(),
                locationId = current.locationId,
                locationName = location?.name.orEmpty(),
                usageNote = current.usageNote.orEmpty(),
                contraindicationNote = current.contraindicationNote.orEmpty(),
                precautionNote = current.precautionNote.orEmpty(),
                userNote = current.userNote.orEmpty(),
                photoPath = current.photoPath,
            ),
            errorMessage = null,
            operationMessage = null,
        )
    }

    fun cancelEdit() {
        _uiState.value = _uiState.value.copy(editingForm = null, errorMessage = null)
    }

    fun updateEditForm(block: DetailEditFormState.() -> DetailEditFormState) {
        val current = _uiState.value.editingForm ?: return
        _uiState.value = _uiState.value.copy(editingForm = current.block())
    }

    fun saveEdit() {
        val form = _uiState.value.editingForm ?: return
        val validation = validateForm(form)
        if (validation != null) {
            _uiState.value = _uiState.value.copy(errorMessage = validation)
            return
        }

        val medicine = _uiState.value.medicine ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true, errorMessage = null)
            runCatching {
                medicineRepository.updateMedicine(
                    medicine.copy(
                        name = form.name.trim(),
                        specification = form.specification.trim().ifBlank { null },
                        manufacturer = form.manufacturer.trim().ifBlank { null },
                        batchNo = form.batchNo.trim().ifBlank { null },
                        productionDate = parseDateOrNull(form.productionDate),
                        expiryDate = parseDateOrNull(form.expiryDate),
                        locationId = form.locationId,
                        usageNote = form.usageNote.trim().ifBlank { null },
                        contraindicationNote = form.contraindicationNote.trim().ifBlank { null },
                        precautionNote = form.precautionNote.trim().ifBlank { null },
                        userNote = form.userNote.trim().ifBlank { null },
                        photoPath = form.photoPath,
                        updatedAt = System.currentTimeMillis(),
                    )
                )
            }.onSuccess {
                reload()
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    saving = false,
                    errorMessage = throwable.message ?: "保存失败，请稍后重试"
                )
            }
        }
    }

    fun submitStockOperation(type: StockOperationType, amountText: String, note: String = "") {
        val amount = amountText.trim().toIntOrNull()
        if (amount == null || amount <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "数量必须是正整数")
            return
        }
        val medicine = _uiState.value.medicine ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true, errorMessage = null)
            runCatching {
                when (type) {
                    StockOperationType.PURCHASE -> medicineRepository.purchaseStock(medicine.id, amount, note)
                    StockOperationType.INCREASE -> medicineRepository.addStock(medicine.id, amount, note)
                    StockOperationType.USE -> medicineRepository.useStock(medicine.id, amount, note)
                    StockOperationType.DISCARD -> medicineRepository.discardStock(medicine.id, amount, note)
                    StockOperationType.DECREASE -> medicineRepository.decreaseStock(medicine.id, amount, note)
                    StockOperationType.ADJUST -> medicineRepository.adjustStock(medicine.id, amount, note)
                }
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    saving = false,
                    operationMessage = "库存已更新"
                )
                reload()
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    saving = false,
                    errorMessage = throwable.message ?: "库存更新失败"
                )
            }
        }
    }

    fun deleteMedicine() {
        val medicine = _uiState.value.medicine ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true, errorMessage = null)
            runCatching {
                medicineRepository.deleteMedicine(medicine)
            }.onSuccess {
                _uiState.value = _uiState.value.copy(saving = false, successMessage = "药品已删除")
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    saving = false,
                    errorMessage = throwable.message ?: "删除失败"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null,
            operationMessage = null,
        )
    }

    private fun buildState(
        medicine: Medicine,
        location: Location?,
        stockRecords: List<StockRecord>,
    ): DetailUiState {
        val expiryStatus = ExpiryChecker.status(medicine.expiryDate)
        return DetailUiState(
            loading = false,
            medicine = medicine,
            location = location,
            expiryStatus = expiryStatus,
            expiryLabel = ExpiryChecker.displayString(medicine.expiryDate),
            stockRecords = stockRecords,
        )
    }

    private fun validateForm(form: DetailEditFormState): String? {
        if (form.name.isBlank()) return "药品名称不能为空"
        if (form.productionDate.isNotBlank() && parseDateOrNull(form.productionDate) == null) {
            return "生产日期格式不正确"
        }
        if (form.expiryDate.isNotBlank() && parseDateOrNull(form.expiryDate) == null) {
            return "有效期格式不正确"
        }
        return null
    }

    private fun parseDateOrNull(value: String): ApproxDate? {
        val text = value.trim()
        if (text.isBlank()) return null
        return runCatching { ApproxDate.parse(text) }.getOrNull()
    }
}

class DetailViewModelFactory(
    private val medicineId: Long,
    private val medicineRepository: MedicineRepository,
    private val locationRepository: LocationRepository,
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DetailViewModel(medicineId, medicineRepository, locationRepository) as T
    }
}
