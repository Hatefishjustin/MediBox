package com.medibox.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.medibox.ui.screens.DetailScreen
import com.medibox.ui.screens.ExpiringScreen
import com.medibox.ui.screens.HomeScreen
import com.medibox.ui.screens.InventoryScreen
import com.medibox.ui.screens.MedicineListScreen
import com.medibox.ui.screens.OcrConfirmScreen
import com.medibox.ui.screens.ProfileScreen
import com.medibox.ui.screens.ScanScreen

@Composable
fun AppNavGraph(
    nav: NavHostController,
    modifier: Modifier = Modifier,
    startRoute: String = Routes.HOME,
) {
    NavHost(
        navController = nav,
        startDestination = startRoute,
        modifier = modifier
    ) {
        composable(Routes.HOME) { HomeScreen(nav) }
        composable(
            route = "${Routes.MEDICINE_LIST}?filter={filter}",
            arguments = listOf(
                navArgument("filter") {
                    nullable = true
                    defaultValue = null
                }
            )
        ) { MedicineListScreen(nav) }
        composable(Routes.SCAN) { ScanScreen(nav) }
        composable(
            route = Routes.OCR_CONFIRM_WITH_CANDIDATES,
            arguments = listOf(
                navArgument("name") { nullable = true; defaultValue = null },
                navArgument("specification") { nullable = true; defaultValue = null },
                navArgument("manufacturer") { nullable = true; defaultValue = null },
                navArgument("batchNo") { nullable = true; defaultValue = null },
                navArgument("productionDate") { nullable = true; defaultValue = null },
                navArgument("expiryDate") { nullable = true; defaultValue = null },
            )
        ) { OcrConfirmScreen(nav) }
        composable(Routes.OCR_CONFIRM) { OcrConfirmScreen(nav) }
        composable(
            route = Routes.DETAIL_WITH_ID,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { DetailScreen(nav) }
        composable(Routes.EXPIRING) { ExpiringScreen(nav) }
        composable(Routes.INVENTORY) { InventoryScreen(nav) }
        composable(Routes.PROFILE) { ProfileScreen(nav) }
    }
}
