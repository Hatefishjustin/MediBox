package com.medibox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.medibox.ui.components.AppBottomNav
import com.medibox.ui.navigation.AppNavGraph
import com.medibox.ui.navigation.Routes
import com.medibox.ui.theme.MediBoxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MediBoxTheme {
                MediBoxAppContent()
            }
        }
    }
}

@Composable
private fun MediBoxAppContent() {
    val nav = rememberNavController()
    val navStack by nav.currentBackStackEntryFlow.collectAsState(initial = null)
    val currentRoute = navStack?.destination?.route ?: Routes.HOME
    // 用 startsWith 匹配带查询参数的路由（如 "medicine_list?filter=EXPIRING"）
    val showBottomBar = setOf(
        Routes.HOME,
        Routes.MEDICINE_LIST,
        Routes.INVENTORY,
        Routes.PROFILE
    ).any { currentRoute.startsWith(it) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                AppBottomNav(currentRoute = currentRoute) { route ->
                    nav.navigate(route)
                }
            }
        }
    ) { padding ->
        AppNavGraph(
            nav = nav,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}
