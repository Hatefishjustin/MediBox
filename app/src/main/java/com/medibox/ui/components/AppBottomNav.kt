package com.medibox.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medibox.ui.navigation.Routes
import com.medibox.ui.theme.AppText
import com.medibox.ui.theme.Brand
import com.medibox.ui.theme.Divider
import com.medibox.ui.theme.InkSecondary
import com.medibox.ui.theme.Space2
import com.medibox.ui.theme.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Person

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun AppBottomNav(currentRoute: String, onSelect: (String) -> Unit) {
    val items = listOf(
        BottomNavItem(Routes.HOME, "首页", Icons.Outlined.Home),
        BottomNavItem(Routes.MEDICINE_LIST, "药箱", Icons.Outlined.Medication),
        BottomNavItem(Routes.SCAN, "扫描", Icons.Outlined.DocumentScanner),
        BottomNavItem(Routes.INVENTORY, "盘点", Icons.Outlined.Checklist),
        BottomNavItem(Routes.PROFILE, "我的", Icons.Outlined.Person)
    )
    Surface(color = Surface, modifier = Modifier.fillMaxWidth()) {
        Column {
            HorizontalDivider(color = Divider, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    if (item.route == Routes.SCAN) {
                        AppScanButton(onClick = { onSelect(item.route) })
                    } else {
                        // 用 startsWith 匹配带查询参数的路由（如 "medicine_list?filter=EXPIRING"）
                        val selected = currentRoute.startsWith(item.route)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { onSelect(item.route) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (selected) Brand else InkSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(Space2))
                            Text(
                                item.label,
                                style = AppText.Caption.copy(
                                    color = if (selected) Brand else InkSecondary,
                                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
