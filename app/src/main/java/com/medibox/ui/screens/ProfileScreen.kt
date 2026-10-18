package com.medibox.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.medibox.ui.components.AppListItem
import com.medibox.ui.components.AppTopBar
import com.medibox.ui.theme.InkTertiary
import com.medibox.ui.theme.Space4
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight

@Composable
fun ProfileScreen(nav: NavHostController) {
    var notify by remember { mutableStateOf(true) }

    Scaffold(topBar = { AppTopBar(title = "我的") }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Spacer(Modifier.height(Space4))
            AppListItem(
                title = "通知设置",
                subtitle = "临期提醒",
                trailing = { Switch(checked = notify, onCheckedChange = { notify = it }) }
            )
            AppListItem(
                title = "药箱管理",
                subtitle = "分类与存放位置",
                trailing = { Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = InkTertiary) },
                onClick = {}
            )
            AppListItem(
                title = "数据备份",
                subtitle = "导出与恢复",
                trailing = { Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = InkTertiary) },
                onClick = {}
            )
            AppListItem(
                title = "关于",
                subtitle = "药箱管家",
                trailing = { Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = InkTertiary) },
                onClick = {}
            )
        }
    }
}
