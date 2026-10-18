package com.medibox.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.medibox.data.local.db.entity.Location
import com.medibox.data.local.db.entity.Medicine
import com.medibox.data.repository.LocationRepository
import com.medibox.data.repository.MedicineRepository
import com.medibox.domain.ExpiryChecker
import com.medibox.domain.ExpiryStatus
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn

/** 有效期筛选（基于 Domain ExpiryChecker，不是假分类）。 */
enum class ExpiryFilter { ALL, EXPIRING, EXPIRED }

/** 列表行数据（UI 渲染用）。 */
data class MedicineRowData(
    val id: Long,
    val name: String,
    val specAndStock: String,
    val locationName: String?,
    val expiryDisplay: String?,
    val expiryStatus: ExpiryStatus,
)

data class MedicineListUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val filter: ExpiryFilter = ExpiryFilter.ALL,
    val rows: List<MedicineRowData> = emptyList(),
    val totalCount: Int = 0,
    val error: String? = null,
)

class MedicineListViewModel(
    private val medicineRepository: MedicineRepository,
    private val locationRepository: LocationRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _filter = MutableStateFlow(ExpiryFilter.ALL)

    @OptIn(FlowPreview::class)
    val uiState: StateFlow<MedicineListUiState> = combine(
        medicineRepository.observeMedicines(),
        locationRepository.observeLocations(),
        _searchQuery.debounce(300),
        _filter,
    ) { medicines, locations, query, filter ->
        computeState(medicines, locations, query, filter)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MedicineListUiState(isLoading = true)
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: ExpiryFilter) {
        _filter.value = filter
    }

    private fun computeState(
        allMedicines: List<Medicine>,
        locations: List<Location>,
        query: String,
        filter: ExpiryFilter,
    ): MedicineListUiState {
        val locMap = locations.associateBy { it.id }
        val totalCount = allMedicines.size

        // 搜索（本地过滤：名称、规格、厂家、批号）
        val searchResults = if (query.isBlank()) {
            allMedicines
        } else {
            allMedicines.filter { m ->
                m.name.contains(query, ignoreCase = true) ||
                m.specification?.contains(query, ignoreCase = true) == true ||
                m.manufacturer?.contains(query, ignoreCase = true) == true ||
                m.batchNo?.contains(query, ignoreCase = true) == true
            }
        }

        // 有效期筛选
        val filtered = searchResults.filter { m ->
            val status = ExpiryChecker.status(m.expiryDate)
            when (filter) {
                ExpiryFilter.ALL -> true
                ExpiryFilter.EXPIRING ->
                    status == ExpiryStatus.EXPIRING_SOON || status == ExpiryStatus.EXPIRED
                ExpiryFilter.EXPIRED ->
                    status == ExpiryStatus.EXPIRED
            }
        }

        val rows = filtered.map { m ->
            MedicineRowData(
                id = m.id,
                name = m.name,
                specAndStock = buildSpecAndStock(m),
                locationName = m.locationId?.let { locMap[it]?.name },
                expiryDisplay = ExpiryChecker.displayString(m.expiryDate),
                expiryStatus = ExpiryChecker.status(m.expiryDate),
            )
        }

        return MedicineListUiState(
            isLoading = false,
            searchQuery = query,
            filter = filter,
            rows = rows,
            totalCount = totalCount,
            error = null,
        )
    }

    private fun buildSpecAndStock(m: Medicine): String {
        val parts = mutableListOf<String>()
        m.specification?.let { parts.add(it) }
        parts.add("${m.stock}盒")
        return parts.joinToString(" · ")
    }
}

class MedicineListViewModelFactory(
    private val medicineRepository: MedicineRepository,
    private val locationRepository: LocationRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MedicineListViewModel::class.java)) {
            return MedicineListViewModel(medicineRepository, locationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
