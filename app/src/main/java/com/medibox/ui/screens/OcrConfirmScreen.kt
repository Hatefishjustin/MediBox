package com.medibox.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.medibox.MediBoxApp
import com.medibox.data.local.db.entity.Location
import com.medibox.data.repository.LocationRepository
import com.medibox.data.repository.MedicineRepository
import com.medibox.domain.ocr.CandidateParser
import com.medibox.domain.ocr.OcrCandidate
import com.medibox.domain.ocr.OcrFieldCandidate
import com.medibox.ui.components.AppButtonGhost
import com.medibox.ui.components.AppButtonPrimary
import com.medibox.ui.components.AppButtonSecondary
import com.medibox.ui.components.ApproxDateField
import com.medibox.ui.components.toReadableApproxDateOrSelf
import com.medibox.ui.components.AppTextField
import com.medibox.ui.components.AppTopBar
import com.medibox.ui.navigation.Routes
import com.medibox.ui.theme.AppText
import com.medibox.ui.theme.Danger
import com.medibox.ui.theme.Ink
import com.medibox.ui.theme.InkSecondary
import com.medibox.ui.theme.Paper
import com.medibox.ui.theme.RadiusMd
import com.medibox.ui.theme.ScreenPaddingX
import com.medibox.ui.theme.SectionGap
import com.medibox.ui.theme.Space2
import com.medibox.ui.theme.Space3
import com.medibox.ui.theme.Space4
import com.medibox.ui.theme.SurfaceVariant
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun OcrConfirmScreen(nav: NavHostController) {
    val backStackEntry by nav.currentBackStackEntryAsState()
    val candidate = remember(backStackEntry) { backStackEntry?.toOcrCandidate() }
    val app = LocalContext.current.applicationContext as MediBoxApp
    val vm: OcrConfirmViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = OcrConfirmViewModelFactory(
            medicineRepository = app.medicineRepository,
            locationRepository = app.locationRepository,
            candidate = candidate,
        )
    )
    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(uiState) {
        val success = uiState as? OcrConfirmUiState.Success ?: return@LaunchedEffect
        nav.navigate("${Routes.DETAIL}/${success.medicineId}") {
            popUpTo(Routes.SCAN) { inclusive = false }
            launchSingleTop = true
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = "确认药品信息", onBack = { nav.popBackStack() }) }
    ) { padding ->
        when (val state = uiState) {
            OcrConfirmUiState.Loading -> LoadingContent(modifier = Modifier.padding(padding))
            is OcrConfirmUiState.Saving -> EditingContent(
                uiState = state.form,
                onNameChanged = vm::onNameChanged,
                onSpecificationChanged = vm::onSpecificationChanged,
                onManufacturerChanged = vm::onManufacturerChanged,
                onBatchNoChanged = vm::onBatchNoChanged,
                onProductionDateChanged = vm::onProductionDateChanged,
                onExpiryDateChanged = vm::onExpiryDateChanged,
                onSave = vm::save,
                onManualEntry = vm::setManualEntryMode,
                onBackToScan = { nav.popBackStack(Routes.SCAN, inclusive = false) },
                padding = padding,
                saving = true,
            )
            is OcrConfirmUiState.Editing -> EditingContent(
                uiState = state.form,
                onNameChanged = vm::onNameChanged,
                onSpecificationChanged = vm::onSpecificationChanged,
                onManufacturerChanged = vm::onManufacturerChanged,
                onBatchNoChanged = vm::onBatchNoChanged,
                onProductionDateChanged = vm::onProductionDateChanged,
                onExpiryDateChanged = vm::onExpiryDateChanged,
                onSave = vm::save,
                onManualEntry = vm::setManualEntryMode,
                onBackToScan = { nav.popBackStack() },
                padding = padding,
                saving = false,
            )
            is OcrConfirmUiState.Error -> EditingContent(
                uiState = state.form,
                errorMessage = state.message,
                onNameChanged = vm::onNameChanged,
                onSpecificationChanged = vm::onSpecificationChanged,
                onManufacturerChanged = vm::onManufacturerChanged,
                onBatchNoChanged = vm::onBatchNoChanged,
                onProductionDateChanged = vm::onProductionDateChanged,
                onExpiryDateChanged = vm::onExpiryDateChanged,
                onSave = vm::save,
                onManualEntry = vm::setManualEntryMode,
                onBackToScan = { nav.popBackStack() },
                padding = padding,
                saving = false,
            )
            is OcrConfirmUiState.Success -> Unit
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = ScreenPaddingX),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "正在准备确认页…", style = AppText.Body, color = InkSecondary)
    }
}

@Composable
private fun EditingContent(
    uiState: OcrConfirmFormState,
    onNameChanged: (String) -> Unit,
    onSpecificationChanged: (String) -> Unit,
    onManufacturerChanged: (String) -> Unit,
    onBatchNoChanged: (String) -> Unit,
    onProductionDateChanged: (String) -> Unit,
    onExpiryDateChanged: (String) -> Unit,
    onSave: () -> Unit,
    onManualEntry: () -> Unit,
    onBackToScan: () -> Unit,
    padding: androidx.compose.foundation.layout.PaddingValues,
    saving: Boolean,
    errorMessage: String? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPaddingX),
        verticalArrangement = Arrangement.spacedBy(SectionGap)
    ) {
        Spacer(Modifier.height(Space2))

        if (errorMessage != null) {
            ErrorBanner(message = errorMessage)
        }

        OcrFieldSection(
            title = "药品名称",
            value = uiState.name,
            onValueChange = onNameChanged,
            candidates = uiState.nameCandidates,
            required = true,
        )
        OcrFieldSection(
            title = "规格",
            value = uiState.specification,
            onValueChange = onSpecificationChanged,
            candidates = uiState.specificationCandidates,
        )
        OcrFieldSection(
            title = "生产厂家",
            value = uiState.manufacturer,
            onValueChange = onManufacturerChanged,
            candidates = uiState.manufacturerCandidates,
        )
        OcrFieldSection(
            title = "批号",
            value = uiState.batchNo,
            onValueChange = onBatchNoChanged,
            candidates = uiState.batchNoCandidates,
        )
        OcrFieldSection(
            title = "生产日期",
            value = uiState.productionDate,
            onValueChange = onProductionDateChanged,
            candidates = uiState.productionDateCandidates,
        )
        OcrFieldSection(
            title = "有效期",
            value = uiState.expiryDate,
            onValueChange = onExpiryDateChanged,
            candidates = uiState.expiryDateCandidates,
        )
        LocationSection(locationName = uiState.locationName)
        PhotoSection()

        AppButtonPrimary(
            text = if (saving) "保存中…" else "保存到药箱",
            onClick = onSave,
            enabled = !saving,
        )
        AppButtonSecondary(text = "返回扫描", onClick = onBackToScan, enabled = !saving)
        AppButtonGhost(text = "手动录入", onClick = onManualEntry)
        Spacer(Modifier.height(Space4))
    }
}

@Composable
private fun OcrFieldSection(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    candidates: List<OcrFieldCandidate>,
    required: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Space3)) {
        Text(
            text = if (required) "$title *" else title,
            style = AppText.Label,
            color = Ink,
        )
        if (title == "生产日期" || title == "有效期") {
            ApproxDateField(
                label = "",
                value = value,
                onValueChange = onValueChange,
                placeholder = "选择日期"
            )
        } else {
            AppTextField(
                value = value,
                onValueChange = onValueChange,
                label = null,
                placeholder = "选填",
            )
        }
        if (candidates.isNotEmpty()) {
            Text(
                text = "OCR 候选：${candidates.joinToString("、") { if (title == "生产日期" || title == "有效期") it.value.toReadableApproxDateOrSelf() else it.value }}",
                style = AppText.Caption,
                color = InkSecondary,
            )
        }
    }
}

@Composable
private fun LocationSection(locationName: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Space3)) {
        Text(text = "位置", style = AppText.Label, color = Ink)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceVariant, RadiusMd)
                .padding(horizontal = Space4, vertical = Space4)
        ) {
            Text(
                text = if (locationName.isBlank()) "我的药箱" else locationName,
                style = AppText.Body,
                color = Ink,
            )
        }
    }
}

@Composable
private fun PhotoSection() {
    Column(verticalArrangement = Arrangement.spacedBy(Space3)) {
        Text(text = "照片入口", style = AppText.Label, color = Ink)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Paper, RoundedCornerShape(12.dp))
                .padding(horizontal = Space4, vertical = Space4)
        ) {
            Text(
                text = "暂无照片",
                style = AppText.Body,
                color = InkSecondary,
            )
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = Space4, vertical = Space4)
    ) {
        Text(text = message, style = AppText.Body, color = Danger, textAlign = TextAlign.Start)
    }
}

private fun NavBackStackEntry.toOcrCandidate(): OcrCandidate? {
    val args = arguments ?: return null
    val name = args.getString("name").orEmpty()
    val specification = args.getString("specification").orEmpty()
    val manufacturer = args.getString("manufacturer").orEmpty()
    val batchNo = args.getString("batchNo").orEmpty()
    val productionDate = args.getString("productionDate").orEmpty()
    val expiryDate = args.getString("expiryDate").orEmpty()
    val fields = listOf(name, specification, manufacturer, batchNo, productionDate, expiryDate)
    if (fields.all { it.isBlank() }) return null
    return OcrCandidate(
        nameCandidates = if (name.isBlank()) emptyList() else listOf(OcrFieldCandidate(name)),
        specificationCandidates = if (specification.isBlank()) emptyList() else listOf(OcrFieldCandidate(specification)),
        manufacturerCandidates = if (manufacturer.isBlank()) emptyList() else listOf(OcrFieldCandidate(manufacturer)),
        batchNoCandidates = if (batchNo.isBlank()) emptyList() else listOf(OcrFieldCandidate(batchNo)),
        productionDateCandidates = normalizeDateCandidates(productionDate),
        expiryDateCandidates = normalizeDateCandidates(expiryDate),
    )
}

private fun normalizeDateCandidates(raw: String): List<OcrFieldCandidate> {
    if (raw.isBlank()) return emptyList()
    val normalized = CandidateParser.parseApproxDateCandidates(raw)
    return if (normalized.isNotEmpty()) {
        normalized.map { OcrFieldCandidate(it.toStorageString()) }
    } else {
        listOf(OcrFieldCandidate(raw))
    }
}

class OcrConfirmViewModelFactory(
    private val medicineRepository: MedicineRepository,
    private val locationRepository: LocationRepository,
    private val candidate: OcrCandidate?,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return OcrConfirmViewModel(medicineRepository, locationRepository, candidate) as T
    }
}
