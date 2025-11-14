package com.ilseon.ui.navigation

sealed class Screen(val route: String) {
    object DailyDashboard : Screen("daily_dashboard")
    object QuickCapture : Screen("quick_capture")
    object About : Screen("about")
    object Settings : Screen("settings")
    object Notes : Screen("notes")
}