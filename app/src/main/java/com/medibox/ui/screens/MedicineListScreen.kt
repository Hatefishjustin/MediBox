package com.medibox.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.medibox.MediBoxApp
import com.medibox.domain.ExpiryChecker
import com.medibox.domain.ExpiryStatus
import com.medibox.ui.components.AppEmptyState
import com.medibox.ui.components.AppErrorState
import com.medibox.ui.components.AppFilterChip
import com.medibox.ui.components.AppListItem
import com.medibox.ui.components.AppLoadingState
import com.medibox.ui.components.AppTag
import com.medibox.ui.components.AppTextField
import com.medibox.ui.components.AppTopBar
import com.medibox.ui.components.TagType
import com.medibox.ui.list.ExpiryFilter
import com.medibox.ui.list.MedicineListUiState
import com.medibox.ui.list.MedicineListViewModel
import com.medibox.ui.list.MedicineListViewModelFactory
import com.medibox.ui.list.MedicineRowData
import com.medibox.ui.navigation.Routes
import com.medibox.ui.theme.AppText
import com.medibox.ui.theme.InkSecondary
import com.medibox.ui.theme.ScreenPaddingX
import com.medibox.ui.theme.Space2
import com.medibox.ui.theme.Space3
import com.medibox.ui.theme.Space4

@Composable
fun MedicineListScreen(nav: NavHostController) {
    val app = LocalContext.current.applicationContext as MediBoxApp
    val viewModel: MedicineListViewModel = viewModel(
        factory = MedicineListViewModelFactory(app.medicineRepository, app.locationRepository)
    )
    val state by viewModel.uiState.collectAsState()

    // 支持路由参数传入初始 filter（如从首页"临期"跳转过来）
    val navBackStackEntry = nav.currentBackStackEntry
    val initialFilter = navBackStackEntry?.arguments?.getString("filter")
    if (initialFilter != null && state.filter == ExpiryFilter.ALL) {
        when (initialFilter) {
            "EXPIRING" -> viewModel.setFilter(ExpiryFilter.EXPIRING)
            "EXPIRED" -> viewModel.setFilter(ExpiryFilter.EXPIRED)
        }
    }

    Scaffold(topBar = { AppTopBar(title = "药箱") }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = ScreenPaddingX)
        ) {
            Spacer(Modifier.height(Space4))

            // 搜索框
            AppTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = "搜索药品名称、厂家、批号"
            )
            Spacer(Modifier.height(Space3))

            // 有效期筛选 chips
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Space2)
            ) {
                AppFilterChip(
                    text = "全部",
                    selected = state.filter == ExpiryFilter.ALL,
                    onClick = { viewModel.setFilter(ExpiryFilter.ALL) }
                )
                AppFilterChip(
                    text = "临期",
                    selected = state.filter == ExpiryFilter.EXPIRING,
                    onClick = { viewModel.setFilter(ExpiryFilter.EXPIRING) }
                )
                AppFilterChip(
                    text = "过期",
                    selected = state.filter == ExpiryFilter.EXPIRED,
                    onClick = { viewModel.setFilter(ExpiryFilter.EXPIRED) }
                )
            }
            Spacer(Modifier.height(Space4))

            // 内容区
            when {
                state.isLoading -> AppLoadingState()
                state.error != null -> AppErrorState(
                    message = "药品读取失败了",
                    onRetry = { /* Flow 自动重试 */ }
                )
                state.totalCount == 0 -> AppEmptyState(
                    title = "你的药箱还是空的",
                    subtitle = "扫描第一件药品，几秒就能记下来。",
                    action = {
                        com.medibox.ui.components.AppButtonPrimary(
                            "扫描药品",
                            onClick = { nav.navigate(Routes.SCAN) }
                        )
                    }
                )
                state.rows.isEmpty() && state.searchQuery.isNotBlank() -> AppEmptyState(
                    title = "没有找到\u201C${state.searchQuery}\u201D",
                    subtitle = "试试其他名称、厂家或批号"
                )
                state.rows.isEmpty() && state.filter != ExpiryFilter.ALL -> AppEmptyState(
                    title = when (state.filter) {
                        ExpiryFilter.EXPIRING -> "暂时没有临期的药"
                        ExpiryFilter.EXPIRED -> "没有过期的药"
                        else -> "没有匹配的药品"
                    },
                    subtitle = when (state.filter) {
                        ExpiryFilter.EXPIRING -> "药品快过期时会在这里提醒你。"
                        ExpiryFilter.EXPIRED -> null
                        else -> null
                    }
                )
                else -> MedicineList(
                    rows = state.rows,
                    nav = nav,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun MedicineList(
    rows: List<MedicineRowData>,
    nav: NavHostController,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        rows.forEach { row ->
            MedicineRow(
                row = row,
                onClick = { nav.navigate("${Routes.DETAIL}/${row.id}") }
            )
        }
    }
}

/** 药品行：两行布局（名称+有效期 / 规格·库存·位置），非卡片。 */
@Composable
private fun MedicineRow(
    row: MedicineRowData,
    onClick: () -> Unit,
) {
    AppListItem(
        title = row.name,
        titleMaxLines = 1,
        subtitle = buildSubtitle(row),
        subtitleMaxLines = 1,
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                if (row.expiryDisplay != null) {
                    androidx.compose.material3.Text(
                        row.expiryDisplay,
                        style = AppText.Caption,
                        color = InkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val tagText = ExpiryChecker.tagText(row.expiryStatus)
                if (tagText != null) {
                    Spacer(Modifier.height(Space2))
                    AppTag(
                        text = tagText,
                        type = when (row.expiryStatus) {
                            ExpiryStatus.EXPIRED -> TagType.Danger
                            ExpiryStatus.EXPIRING_SOON -> TagType.Warning
                            else -> TagType.Neutral
                        }
                    )
                }
            }
        },
        onClick = onClick,
        showDivider = true
    )
}

/** 拼装副标题：规格 · 库存 · (位置)。 */
private fun buildSubtitle(row: MedicineRowData): String {
    val parts = mutableListOf<String>()
    parts.add(row.specAndStock)
    row.locationName?.let { parts.add(it) }
    return parts.joinToString(" · ")
}
