package com.medibox.ui.screens

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.currentBackStackEntryAsState
import com.medibox.MediBoxApp
import com.medibox.data.local.db.entity.StockRecord
import com.medibox.domain.ExpiryChecker
import com.medibox.domain.ExpiryStatus
import com.medibox.domain.StockChangeType
import com.medibox.ui.components.AppButtonGhost
import com.medibox.ui.components.AppButtonPrimary
import com.medibox.ui.components.AppButtonSecondary
import com.medibox.ui.components.AppEmptyState
import com.medibox.ui.components.ApproxDateField
import com.medibox.ui.components.AppTextField
import com.medibox.ui.components.AppTopBar
import com.medibox.ui.components.toHumanText
import com.medibox.ui.navigation.Routes
import com.medibox.ui.theme.AppText
import com.medibox.ui.theme.Danger
import com.medibox.ui.theme.DangerSoft
import com.medibox.ui.theme.Divider
import com.medibox.ui.theme.Ink
import com.medibox.ui.theme.InkSecondary
import com.medibox.ui.theme.InkTertiary
import com.medibox.ui.theme.Ok
import com.medibox.ui.theme.OkSoft
import com.medibox.ui.theme.Paper
import com.medibox.ui.theme.RadiusMd
import com.medibox.ui.theme.ScreenPaddingX
import com.medibox.ui.theme.SectionGap
import com.medibox.ui.theme.Space2
import com.medibox.ui.theme.Space3
import com.medibox.ui.theme.Space4
import com.medibox.ui.theme.Surface
import com.medibox.ui.theme.SurfaceVariant
import com.medibox.ui.theme.Warning
import com.medibox.ui.theme.WarningSoft
import java.util.Locale

@Composable
fun DetailScreen(nav: NavHostController) {
    val backStackEntry by nav.currentBackStackEntryAsState()
    val medicineId = remember(backStackEntry) { backStackEntry?.arguments?.getLong("id") ?: -1L }
    val app = LocalContext.current.applicationContext as MediBoxApp
    val vm: DetailViewModel = viewModel(
        factory = DetailViewModelFactory(
            medicineId = medicineId,
            medicineRepository = app.medicineRepository,
            locationRepository = app.locationRepository,
        )
    )
    val uiState by vm.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage == "药品已删除") {
            nav.popBackStack(Routes.MEDICINE_LIST, inclusive = false)
            vm.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "药品详情",
                onBack = { nav.popBackStack() },
                actions = {
                    TextButton(onClick = { vm.enterEditMode() }) { Text("编辑") }
                    TextButton(onClick = { showDeleteDialog = true }) { Text("删除") }
                }
            )
        }
    ) { padding ->
        if (uiState.loading) {
            DetailLoading(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        uiState.errorMessage?.let { message ->
            DetailError(
                message = message,
                onRetry = { vm.reload() },
                onBack = { nav.popBackStack() },
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        val medicine = uiState.medicine
        if (medicine == null) {
            DetailError(
                message = "药品不存在或已被删除",
                onRetry = { vm.reload() },
                onBack = { nav.popBackStack() },
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        if (uiState.editingForm != null) {
            DetailEditContent(
                padding = padding,
                state = uiState,
                onNameChanged = { vm.updateEditForm { copy(name = it) } },
                onSpecificationChanged = { vm.updateEditForm { copy(specification = it) } },
                onManufacturerChanged = { vm.updateEditForm { copy(manufacturer = it) } },
                onBatchNoChanged = { vm.updateEditForm { copy(batchNo = it) } },
                onProductionDateChanged = { vm.updateEditForm { copy(productionDate = it) } },
                onExpiryDateChanged = { vm.updateEditForm { copy(expiryDate = it) } },
                onUsageNoteChanged = { vm.updateEditForm { copy(usageNote = it) } },
                onContraindicationChanged = { vm.updateEditForm { copy(contraindicationNote = it) } },
                onPrecautionChanged = { vm.updateEditForm { copy(precautionNote = it) } },
                onUserNoteChanged = { vm.updateEditForm { copy(userNote = it) } },
                onSave = vm::saveEdit,
                onCancel = vm::cancelEdit,
                onLocationSelected = { location -> vm.updateEditForm { copy(locationId = location?.id, locationName = location?.name.orEmpty()) } },
                onPhotoPathChanged = { vm.updateEditForm { copy(photoPath = it) } },
            )
        } else {
            DetailContent(
                padding = padding,
                state = uiState,
                onEdit = { vm.enterEditMode() },
                onIncrease = { vm.submitStockOperation(StockOperationType.INCREASE, "1") },
                onPurchase = { vm.submitStockOperation(StockOperationType.PURCHASE, "1") },
                onUse = { vm.submitStockOperation(StockOperationType.USE, "1") },
                onDiscard = { vm.submitStockOperation(StockOperationType.DISCARD, "1") },
                onDecrease = { vm.submitStockOperation(StockOperationType.DECREASE, "1") },
                onAdjust = { vm.submitStockOperation(StockOperationType.ADJUST, medicine.stock.toString()) },
                onClearMessage = vm::clearMessages,
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("删除药品") },
                text = { Text("药品会被删除，对应库存流水和照片文件也会一起清理，此操作不可恢复。") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        vm.deleteMedicine()
                    }) { Text("删除") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
                }
            )
        }
    }
}

@Composable
private fun DetailLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "正在读取药品详情…", style = AppText.Body, color = InkSecondary)
    }
}

@Composable
private fun DetailError(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().padding(horizontal = ScreenPaddingX),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space4)) {
            AppEmptyState(title = message, subtitle = "你可以返回列表，或者重试一次。")
            AppButtonPrimary(text = "重试", onClick = onRetry)
            AppButtonGhost(text = "返回列表", onClick = onBack)
        }
    }
}

@Composable
private fun DetailContent(
    padding: androidx.compose.foundation.layout.PaddingValues,
    state: DetailUiState,
    onEdit: () -> Unit,
    onIncrease: () -> Unit,
    onPurchase: () -> Unit,
    onUse: () -> Unit,
    onDiscard: () -> Unit,
    onDecrease: () -> Unit,
    onAdjust: () -> Unit,
    onClearMessage: () -> Unit,
) {
    val medicine = state.medicine ?: return
    val expiryStatus = state.expiryStatus
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPaddingX),
        verticalArrangement = Arrangement.spacedBy(SectionGap)
    ) {
        state.operationMessage?.let {
            DetailNotice(text = it, onDismiss = onClearMessage, color = Ok, background = OkSoft)
        }
        state.successMessage?.let {
            DetailNotice(text = it, onDismiss = onClearMessage, color = Ok, background = OkSoft)
        }
        state.errorMessage?.let {
            DetailNotice(text = it, onDismiss = onClearMessage, color = Danger, background = DangerSoft)
        }

        HeaderSection(medicine.name, medicine.specification, expiryStatus)
        SummarySection(state)
        ActionSection(onEdit, onIncrease, onPurchase, onUse, onDiscard, onDecrease, onAdjust)
        PhotoSection(medicine.photoPath)
        ManufacturerSection(medicine.manufacturer, medicine.batchNo, medicine.productionDate?.let { it.toHumanText() })
        NotesSection(medicine.usageNote, medicine.contraindicationNote, medicine.precautionNote, medicine.userNote)
        StockRecordSection(state.stockRecords)
    }
}

@Composable
private fun DetailEditContent(
    padding: androidx.compose.foundation.layout.PaddingValues,
    state: DetailUiState,
    onNameChanged: (String) -> Unit,
    onSpecificationChanged: (String) -> Unit,
    onManufacturerChanged: (String) -> Unit,
    onBatchNoChanged: (String) -> Unit,
    onProductionDateChanged: (String) -> Unit,
    onExpiryDateChanged: (String) -> Unit,
    onUsageNoteChanged: (String) -> Unit,
    onContraindicationChanged: (String) -> Unit,
    onPrecautionChanged: (String) -> Unit,
    onUserNoteChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onLocationSelected: (com.medibox.data.local.db.entity.Location?) -> Unit,
    onPhotoPathChanged: (String?) -> Unit,
) {
    val form = state.editingForm ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPaddingX),
        verticalArrangement = Arrangement.spacedBy(SectionGap)
    ) {
        state.errorMessage?.let {
            DetailNotice(text = it, onDismiss = {}, color = Danger, background = DangerSoft)
        }
        Text(text = "编辑药品", style = AppText.Title, color = Ink)
        AppTextField(value = form.name, onValueChange = onNameChanged, label = "药品名称", placeholder = "必填")
        AppTextField(value = form.specification, onValueChange = onSpecificationChanged, label = "规格", placeholder = "选填")
        AppTextField(value = form.manufacturer, onValueChange = onManufacturerChanged, label = "生产厂家", placeholder = "选填")
        AppTextField(value = form.batchNo, onValueChange = onBatchNoChanged, label = "批号", placeholder = "选填")
        ApproxDateField(label = "生产日期", value = form.productionDate, onValueChange = onProductionDateChanged)
        ApproxDateField(label = "有效期", value = form.expiryDate, onValueChange = onExpiryDateChanged)
        AppTextField(value = form.usageNote, onValueChange = onUsageNoteChanged, label = "用法用量", placeholder = "用户保存的信息")
        AppTextField(value = form.contraindicationNote, onValueChange = onContraindicationChanged, label = "禁忌备注", placeholder = "用户保存的信息")
        AppTextField(value = form.precautionNote, onValueChange = onPrecautionChanged, label = "注意事项", placeholder = "用户保存的信息")
        AppTextField(value = form.userNote, onValueChange = onUserNoteChanged, label = "我的备注", placeholder = "用户保存的信息")
        LocationEditRow(form.locationName, onLocationSelected)
        PhotoEditRow(form.photoPath, onPhotoPathChanged)
        AppButtonPrimary(text = "保存修改", onClick = onSave, enabled = !state.saving)
        AppButtonGhost(text = "取消编辑", onClick = onCancel)
    }
}

@Composable
private fun HeaderSection(name: String, specification: String?, expiryStatus: ExpiryStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(Space2)) {
        Text(text = name, style = AppText.Title, color = Ink)
        if (!specification.isNullOrBlank()) {
            Text(text = specification, style = AppText.Body, color = InkSecondary)
        }
        ExpiryStatusChip(expiryStatus)
    }
}

@Composable
private fun ExpiryStatusChip(status: ExpiryStatus) {
    val text = when (status) {
        ExpiryStatus.EXPIRED -> "已过期"
        ExpiryStatus.EXPIRING_SOON -> "临期"
        ExpiryStatus.NORMAL -> "正常"
        ExpiryStatus.UNKNOWN -> "有效期未知"
    }
    val background = when (status) {
        ExpiryStatus.EXPIRED -> DangerSoft
        ExpiryStatus.EXPIRING_SOON -> WarningSoft
        ExpiryStatus.NORMAL -> SurfaceVariant
        ExpiryStatus.UNKNOWN -> SurfaceVariant
    }
    val color = when (status) {
        ExpiryStatus.EXPIRED -> Danger
        ExpiryStatus.EXPIRING_SOON -> Warning
        ExpiryStatus.NORMAL -> InkSecondary
        ExpiryStatus.UNKNOWN -> InkSecondary
    }
    Box(
        modifier = Modifier
            .background(background, RadiusMd)
            .padding(horizontal = Space3, vertical = Space2)
    ) {
        Text(text = text, style = AppText.Label, color = color)
    }
}

@Composable
private fun SummarySection(state: DetailUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(Space3)) {
        DetailRow("有效期", state.expiryLabel ?: "未填写")
        DetailRow("当前库存", state.medicine?.stock?.toString() ?: "-")
        DetailRow("所属药箱", state.location?.name ?: "未设置位置")
    }
}

@Composable
private fun ActionSection(
    onEdit: () -> Unit,
    onIncrease: () -> Unit,
    onPurchase: () -> Unit,
    onUse: () -> Unit,
    onDiscard: () -> Unit,
    onDecrease: () -> Unit,
    onAdjust: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Space3)) {
        Text(text = "库存操作", style = AppText.Label, color = Ink)
        Row(horizontalArrangement = Arrangement.spacedBy(Space3), modifier = Modifier.fillMaxWidth()) {
            AppButtonSecondary(text = "入库", onClick = onPurchase, modifier = Modifier.weight(1f))
            AppButtonSecondary(text = "增加", onClick = onIncrease, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space3), modifier = Modifier.fillMaxWidth()) {
            AppButtonSecondary(text = "使用", onClick = onUse, modifier = Modifier.weight(1f))
            AppButtonSecondary(text = "丢弃", onClick = onDiscard, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space3), modifier = Modifier.fillMaxWidth()) {
            AppButtonSecondary(text = "减少", onClick = onDecrease, modifier = Modifier.weight(1f))
            AppButtonSecondary(text = "调整", onClick = onAdjust, modifier = Modifier.weight(1f))
        }
        AppButtonGhost(text = "编辑药品信息", onClick = onEdit)
    }
}

@Composable
private fun PhotoSection(photoPath: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(Space3)) {
        Text(text = "照片", style = AppText.Label, color = Ink)
        if (photoPath.isNullOrBlank()) {
            EmptyInline("暂无照片")
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceVariant, RadiusMd)
                    .padding(horizontal = Space4, vertical = Space4)
            ) {
                Text(text = photoPath, style = AppText.Caption, color = InkSecondary)
            }
        }
    }
}

@Composable
private fun ManufacturerSection(manufacturer: String?, batchNo: String?, productionDate: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(Space3)) {
        Text(text = "客观来源", style = AppText.Label, color = Ink)
        if (!manufacturer.isNullOrBlank()) DetailRow("生产厂家", manufacturer)
        if (!batchNo.isNullOrBlank()) DetailRow("批号", batchNo)
        if (!productionDate.isNullOrBlank()) DetailRow("生产日期", productionDate)
        if (manufacturer.isNullOrBlank() && batchNo.isNullOrBlank() && productionDate.isNullOrBlank()) {
            EmptyInline("未填写")
        }
    }
}

@Composable
private fun NotesSection(usageNote: String?, contraindicationNote: String?, precautionNote: String?, userNote: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(Space3)) {
        Text(text = "用户保存的信息", style = AppText.Label, color = Ink)
        if (!usageNote.isNullOrBlank()) DetailRow("用法用量", usageNote)
        if (!contraindicationNote.isNullOrBlank()) DetailRow("禁忌备注", contraindicationNote)
        if (!precautionNote.isNullOrBlank()) DetailRow("注意事项", precautionNote)
        if (!userNote.isNullOrBlank()) DetailRow("我的备注", userNote)
        if (usageNote.isNullOrBlank() && contraindicationNote.isNullOrBlank() && precautionNote.isNullOrBlank() && userNote.isNullOrBlank()) {
            EmptyInline("未填写")
        }
    }
}

@Composable
private fun StockRecordSection(records: List<StockRecord>) {
    Column(verticalArrangement = Arrangement.spacedBy(Space3)) {
        Text(text = "库存流水", style = AppText.Label, color = Ink)
        if (records.isEmpty()) {
            EmptyInline("暂无流水")
            return
        }
        records.takeLast(5).forEach { record ->
            DetailRow(
                label = when (record.changeType) {
                    StockChangeType.PURCHASE -> "入库"
                    StockChangeType.INCREASE -> "增加"
                    StockChangeType.USE -> "使用"
                    StockChangeType.DISCARD -> "丢弃"
                    StockChangeType.DECREASE -> "减少"
                    StockChangeType.ADJUST -> "调整"
                },
                value = "${record.amount}，余额 ${record.balance}"
            )
        }
    }
}

@Composable
private fun LocationEditRow(locationName: String, onLocationSelected: (com.medibox.data.local.db.entity.Location?) -> Unit) {
    Text(text = "所属药箱：${if (locationName.isBlank()) "未设置位置" else locationName}", style = AppText.Body, color = InkSecondary)
}

@Composable
private fun PhotoEditRow(photoPath: String?, onPhotoPathChanged: (String?) -> Unit) {
    Text(text = "照片：${photoPath ?: "暂无"}", style = AppText.Body, color = InkSecondary)
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Space2)) {
        Text(text = label, style = AppText.Caption, color = InkSecondary)
        Text(text = value, style = AppText.Body, color = Ink)
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().size(1.dp)) {
            drawLine(color = Divider, start = androidx.compose.ui.geometry.Offset.Zero, end = androidx.compose.ui.geometry.Offset(size.width, 0f))
        }
    }
}

@Composable
private fun EmptyInline(text: String) {
    Text(text = text, style = AppText.Body, color = InkTertiary)
}

@Composable
private fun DetailNotice(text: String, onDismiss: () -> Unit, color: androidx.compose.ui.graphics.Color, background: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RadiusMd)
            .padding(horizontal = Space4, vertical = Space3)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = text, style = AppText.Body, color = color, modifier = Modifier.weight(1f))
            AppButtonGhost(text = "关闭", onClick = onDismiss)
        }
    }
}
