package com.medibox.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.medibox.MediBoxApp
import com.medibox.domain.ExpiryChecker
import com.medibox.domain.ExpiryStatus
import com.medibox.ui.components.AppButtonPrimary
import com.medibox.ui.components.AppButtonSecondary
import com.medibox.ui.components.AppEmptyState
import com.medibox.ui.components.AppErrorState
import com.medibox.ui.components.AppListItem
import com.medibox.ui.components.AppLoadingState
import com.medibox.ui.components.AppTag
import com.medibox.ui.components.AppTopBar
import com.medibox.ui.components.TagType
import com.medibox.ui.home.HomeUiState
import com.medibox.ui.home.HomeViewModel
import com.medibox.ui.home.HomeViewModelFactory
import com.medibox.ui.navigation.Routes
import com.medibox.ui.theme.AppText
import com.medibox.ui.theme.Danger
import com.medibox.ui.theme.Ink
import com.medibox.ui.theme.InkSecondary
import com.medibox.ui.theme.InkTertiary
import com.medibox.ui.theme.ScreenPaddingX
import com.medibox.ui.theme.SectionGap
import com.medibox.ui.theme.Space2
import com.medibox.ui.theme.Space4
import com.medibox.ui.theme.Space8
import com.medibox.ui.theme.Warning

@Composable
fun HomeScreen(nav: NavHostController) {
    val app = LocalContext.current.applicationContext as MediBoxApp
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(app.medicineRepository, app.locationRepository)
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = { AppTopBar(title = "药箱") }) { padding ->
        when {
            state.isLoading -> AppLoadingState(modifier = Modifier.padding(padding))
            state.error != null -> AppErrorState(
                message = "药品读取失败了",
                onRetry = { /* Flow 自动重试 */ },
                modifier = Modifier.padding(padding)
            )
            state.totalCount == 0 -> AppEmptyState(
                title = "你的药箱还是空的",
                subtitle = "扫描第一件药品，几秒就能记下来。",
                action = { AppButtonPrimary("扫描药品", onClick = { nav.navigate(Routes.SCAN) }) },
                modifier = Modifier.padding(padding)
            )
            else -> HomeContent(
                state = state,
                nav = nav,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    nav: NavHostController,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPaddingX)
    ) {
        Spacer(Modifier.height(Space8))

        // Layer 1: 需要关注
        if (state.attention != null && state.attention.hasAttention) {
            if (state.attention.expiredCount > 0) {
                AttentionRow(
                    text = "有 ${state.attention.expiredCount} 个药品已过期",
                    color = Danger,
                    onClick = { nav.navigate("${Routes.MEDICINE_LIST}?filter=EXPIRED") }
                )
            }
            if (state.attention.expiringSoonCount > 0) {
                AttentionRow(
                    text = "有 ${state.attention.expiringSoonCount} 个药品即将到期",
                    color = Warning,
                    onClick = { nav.navigate("${Routes.MEDICINE_LIST}?filter=EXPIRING") }
                )
            }
            Spacer(Modifier.height(SectionGap))
        }

        // Layer 2: 药箱概况
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                state.defaultLocationName,
                style = AppText.TitleSmall,
                color = Ink
            )
            Text(
                " · 共 ${state.totalCount} 种药",
                style = AppText.Secondary,
                color = InkSecondary
            )
        }
        Spacer(Modifier.height(SectionGap))

        // Layer 3: 最近添加
        if (state.recentItems.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("最近添加", style = AppText.Section, color = Ink)
                Text(
                    "全部 →",
                    style = AppText.Label,
                    color = com.medibox.ui.theme.Brand,
                    modifier = Modifier.clickable { nav.navigate(Routes.MEDICINE_LIST) }
                )
            }
            Spacer(Modifier.height(Space2))

            state.recentItems.forEach { item ->
                AppListItem(
                    title = item.name,
                    titleMaxLines = 1,
                    subtitle = item.specAndStock,
                    subtitleMaxLines = 1,
                    trailing = {
                        Column(horizontalAlignment = Alignment.End) {
                            if (item.expiryDisplay != null) {
                                Text(
                                    item.expiryDisplay,
                                    style = AppText.Caption,
                                    color = InkSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            val tagText = ExpiryChecker.tagText(item.expiryStatus)
                            if (tagText != null) {
                                Spacer(Modifier.height(Space2))
                                AppTag(
                                    text = tagText,
                                    type = when (item.expiryStatus) {
                                        ExpiryStatus.EXPIRED -> TagType.Danger
                                        ExpiryStatus.EXPIRING_SOON -> TagType.Warning
                                        else -> TagType.Neutral
                                    }
                                )
                            }
                        }
                    },
                    onClick = { nav.navigate("${Routes.DETAIL}/${item.id}") },
                    showDivider = true
                )
            }
            Spacer(Modifier.height(SectionGap))
        }

        // Layer 4: 操作入口
        AppButtonSecondary(
            text = "查看全部药品",
            onClick = { nav.navigate(Routes.MEDICINE_LIST) }
        )
        Spacer(Modifier.height(Space4))
        AppButtonPrimary(
            text = "扫描药品",
            onClick = { nav.navigate(Routes.SCAN) }
        )

        Spacer(Modifier.height(Space8))
    }
}

/** Layer 1 关注条目：一行文字 + 右箭头，不是卡片。 */
@Composable
private fun AttentionRow(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Space4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, style = AppText.Secondary, color = color)
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = InkTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}
