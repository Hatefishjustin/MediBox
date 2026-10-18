package com.medibox.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.medibox.ui.components.AppEmptyState
import com.medibox.ui.components.AppTopBar

@Composable
fun InventoryScreen(nav: NavHostController) {
    Scaffold(
        topBar = { AppTopBar(title = "盘点", onBack = { nav.popBackStack() }) }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            AppEmptyState(
                title = "没有可盘点的药品",
                subtitle = "添加药品后，可以在这里快速核对库存。"
            )
        }
    }
}
