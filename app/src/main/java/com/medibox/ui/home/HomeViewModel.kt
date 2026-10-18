package com.medibox.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.medibox.data.local.db.entity.Location
import com.medibox.data.local.db.entity.Medicine
import com.medibox.data.repository.LocationRepository
import com.medibox.data.repository.MedicineRepository
import com.medibox.domain.ExpiryChecker
import com.medibox.domain.ExpiryStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** 首页需要关注的条目（仅过期 + 临期，不含库存提醒——S2 暂不定义库存规则）。 */
data class HomeAttention(
    val expiredCount: Int,
    val expiringSoonCount: Int,
) {
    val hasAttention: Boolean get() = expiredCount > 0 || expiringSoonCount > 0
}

/** 首页最近添加条目（UI 渲染用）。 */
data class HomeRecentItem(
    val id: Long,
    val name: String,
    val specAndStock: String,
    val locationName: String?,
    val expiryDisplay: String?,
    val expiryStatus: ExpiryStatus,
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val totalCount: Int = 0,
    val defaultLocationName: String = "我的药箱",
    val attention: HomeAttention? = null,
    val recentItems: List<HomeRecentItem> = emptyList(),
    val error: String? = null,
)

class HomeViewModel(
    private val medicineRepository: MedicineRepository,
    private val locationRepository: LocationRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        medicineRepository.observeMedicines(),
        locationRepository.observeLocations(),
    ) { medicines, locations ->
        computeState(medicines, locations)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    private fun computeState(
        medicines: List<Medicine>,
        locations: List<Location>
    ): HomeUiState {
        val locMap = locations.associateBy { it.id }

        // 按有效期状态分组
        val expired = medicines.filter {
            ExpiryChecker.status(it.expiryDate) == ExpiryStatus.EXPIRED
        }
        val expiringSoon = medicines.filter {
            ExpiryChecker.status(it.expiryDate) == ExpiryStatus.EXPIRING_SOON
        }

        val attention = if (expired.isNotEmpty() || expiringSoon.isNotEmpty()) {
            HomeAttention(
                expiredCount = expired.size,
                expiringSoonCount = expiringSoon.size,
            )
        } else null

        // 最近添加（按 createdAt DESC 取前 3）
        val recent = medicines
            .sortedByDescending { it.createdAt }
            .take(3)
            .map { m ->
                HomeRecentItem(
                    id = m.id,
                    name = m.name,
                    specAndStock = buildSpecAndStock(m),
                    locationName = m.locationId?.let { locMap[it]?.name },
                    expiryDisplay = ExpiryChecker.displayString(m.expiryDate),
                    expiryStatus = ExpiryChecker.status(m.expiryDate),
                )
            }

        return HomeUiState(
            isLoading = false,
            totalCount = medicines.size,
            defaultLocationName = locations.firstOrNull { it.isDefault }?.name ?: "我的药箱",
            attention = attention,
            recentItems = recent,
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

class HomeViewModelFactory(
    private val medicineRepository: MedicineRepository,
    private val locationRepository: LocationRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(medicineRepository, locationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
