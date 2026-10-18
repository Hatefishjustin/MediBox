package com.medibox.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medibox.data.local.db.entity.Location
import com.medibox.data.local.db.entity.Medicine
import com.medibox.data.repository.LocationRepository
import com.medibox.data.repository.MedicineRepository
import com.medibox.domain.ApproxDate
import com.medibox.domain.ocr.OcrCandidate
import com.medibox.domain.ocr.OcrFieldCandidate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface OcrConfirmUiState {
    data object Loading : OcrConfirmUiState
    data class Editing(val form: OcrConfirmFormState) : OcrConfirmUiState
    data class Saving(val form: OcrConfirmFormState) : OcrConfirmUiState
    data class Success(val medicineId: Long) : OcrConfirmUiState
    data class Error(val message: String, val form: OcrConfirmFormState) : OcrConfirmUiState
}

data class OcrConfirmFormState(
    val name: String = "",
    val specification: String = "",
    val manufacturer: String = "",
    val batchNo: String = "",
    val productionDate: String = "",
    val expiryDate: String = "",
    val locationId: Long? = null,
    val locationName: String = "",
    val photoPath: String? = null,
    val sourceCandidates: OcrCandidate? = null,
    val nameCandidates: List<OcrFieldCandidate> = emptyList(),
    val specificationCandidates: List<OcrFieldCandidate> = emptyList(),
    val manufacturerCandidates: List<OcrFieldCandidate> = emptyList(),
    val batchNoCandidates: List<OcrFieldCandidate> = emptyList(),
    val productionDateCandidates: List<OcrFieldCandidate> = emptyList(),
    val expiryDateCandidates: List<OcrFieldCandidate> = emptyList(),
)

class OcrConfirmViewModel(
    private val medicineRepository: MedicineRepository,
    private val locationRepository: LocationRepository,
    candidate: OcrCandidate? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OcrConfirmUiState>(OcrConfirmUiState.Loading)
    val uiState: StateFlow<OcrConfirmUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val defaultLocation = locationRepository.getDefault() ?: run {
                locationRepository.ensureDefaultLocation()
                locationRepository.getDefault()
            }
            _uiState.value = OcrConfirmUiState.Editing(buildInitialForm(candidate, defaultLocation))
        }
    }

    fun onNameChanged(value: String) = updateForm { copy(name = value) }
    fun onSpecificationChanged(value: String) = updateForm { copy(specification = value) }
    fun onManufacturerChanged(value: String) = updateForm { copy(manufacturer = value) }
    fun onBatchNoChanged(value: String) = updateForm { copy(batchNo = value) }
    fun onProductionDateChanged(value: String) = updateForm { copy(productionDate = value) }
    fun onExpiryDateChanged(value: String) = updateForm { copy(expiryDate = value) }
    fun onLocationChanged(location: Location?) = updateForm {
        copy(locationId = location?.id, locationName = location?.name.orEmpty())
    }
    fun onPhotoPathChanged(value: String?) = updateForm { copy(photoPath = value) }

    fun setManualEntryMode() {
        val current = currentForm() ?: return
        _uiState.value = OcrConfirmUiState.Editing(current.copy(sourceCandidates = null))
    }

    fun save() {
        val current = currentForm() ?: return
        val validation = validate(current)
        if (validation != null) {
            _uiState.value = OcrConfirmUiState.Error(validation, current)
            return
        }

        viewModelScope.launch {
            _uiState.value = OcrConfirmUiState.Saving(current)
            runCatching {
                val medicine = Medicine(
                    name = current.name.trim(),
                    specification = current.specification.trim().ifBlank { null },
                    manufacturer = current.manufacturer.trim().ifBlank { null },
                    batchNo = current.batchNo.trim().ifBlank { null },
                    productionDate = parseDateOrNull(current.productionDate),
                    expiryDate = parseDateOrNull(current.expiryDate),
                    locationId = current.locationId,
                    photoPath = current.photoPath,
                )
                medicineRepository.insertMedicine(medicine)
            }.onSuccess { medicineId ->
                _uiState.value = OcrConfirmUiState.Success(medicineId)
            }.onFailure { throwable ->
                _uiState.value = OcrConfirmUiState.Error(
                    message = throwable.message ?: "保存失败，请稍后重试",
                    form = current
                )
            }
        }
    }

    fun currentForm(): OcrConfirmFormState? = when (val state = _uiState.value) {
        is OcrConfirmUiState.Editing -> state.form
        is OcrConfirmUiState.Saving -> state.form
        is OcrConfirmUiState.Error -> state.form
        else -> null
    }

    private fun updateForm(block: OcrConfirmFormState.() -> OcrConfirmFormState) {
        val current = currentForm() ?: return
        _uiState.value = OcrConfirmUiState.Editing(current.block())
    }

    private fun buildInitialForm(candidate: OcrCandidate?, defaultLocation: Location?): OcrConfirmFormState {
        return OcrConfirmFormState(
            name = candidate?.nameCandidates?.firstOrNull()?.value.orEmpty(),
            specification = candidate?.specificationCandidates?.firstOrNull()?.value.orEmpty(),
            manufacturer = candidate?.manufacturerCandidates?.firstOrNull()?.value.orEmpty(),
            batchNo = candidate?.batchNoCandidates?.firstOrNull()?.value.orEmpty(),
            productionDate = candidate?.productionDateCandidates?.firstOrNull()?.value.orEmpty(),
            expiryDate = candidate?.expiryDateCandidates?.firstOrNull()?.value.orEmpty(),
            locationId = defaultLocation?.id,
            locationName = defaultLocation?.name.orEmpty(),
            sourceCandidates = candidate,
            nameCandidates = candidate?.nameCandidates.orEmpty(),
            specificationCandidates = candidate?.specificationCandidates.orEmpty(),
            manufacturerCandidates = candidate?.manufacturerCandidates.orEmpty(),
            batchNoCandidates = candidate?.batchNoCandidates.orEmpty(),
            productionDateCandidates = candidate?.productionDateCandidates.orEmpty(),
            expiryDateCandidates = candidate?.expiryDateCandidates.orEmpty(),
        )
    }

    private fun validate(form: OcrConfirmFormState): String? {
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
