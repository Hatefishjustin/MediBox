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
fun ExpiringScreen(nav: NavHostController) {
    Scaffold(
        topBar = { AppTopBar(title = "临期提醒", onBack = { nav.popBackStack() }) }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            AppEmptyState(
                title = "暂时没有临期的药",
                subtitle = "药品进入临期范围后，会按设置提醒你。"
            )
        }
    }
}
