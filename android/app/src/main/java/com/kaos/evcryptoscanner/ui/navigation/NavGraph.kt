package com.kaos.evcryptoscanner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kaos.evcryptoscanner.ui.screen.DashboardScreen
import com.kaos.evcryptoscanner.ui.screen.HelpScreen
import com.kaos.evcryptoscanner.ui.screen.LinksScreen
import com.kaos.evcryptoscanner.ui.screen.SettingsScreen
import com.kaos.evcryptoscanner.ui.screen.WidgetCustomizerScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Settings : Screen("settings")
    object Widgets : Screen("widgets")
    object Help : Screen("help")
    object Links : Screen("links")
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController)
        }
        composable(Screen.Widgets.route) {
            WidgetCustomizerScreen(navController)
        }
        composable(Screen.Help.route) {
            HelpScreen(navController)
        }
        composable(Screen.Links.route) {
            LinksScreen(navController)
        }
    }
}
