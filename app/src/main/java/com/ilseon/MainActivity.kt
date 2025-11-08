package com.ilseon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ilseon.ui.navigation.Screen
import com.ilseon.ui.screen.AboutScreen
import com.ilseon.ui.screen.DashboardScreen
import com.ilseon.ui.screen.QuickCaptureSheet
import com.ilseon.ui.screen.SettingsScreen
import com.ilseon.ui.theme.IlseonTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: TaskViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContent {
            IlseonTheme {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val tasks by viewModel.tasks.collectAsState()

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            NavigationDrawerItem(
                                label = { Text("Dashboard") },
                                selected = currentRoute == Screen.DailyDashboard.route,
                                onClick = {
                                    navController.navigate(Screen.DailyDashboard.route)
                                    scope.launch { drawerState.close() }
                                }
                            )
                            NavigationDrawerItem(
                                label = { Text("Settings") },
                                selected = currentRoute == Screen.Settings.route,
                                onClick = {
                                    navController.navigate(Screen.Settings.route)
                                    scope.launch { drawerState.close() }
                                }
                            )
                            NavigationDrawerItem(
                                label = { Text("About") },
                                selected = currentRoute == Screen.About.route,
                                onClick = {
                                    navController.navigate(Screen.About.route)
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Ilseon") },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                                    }
                                }
                            )
                        },
                        floatingActionButton = {
                            if (currentRoute == Screen.DailyDashboard.route) {
                                FloatingActionButton(
                                    onClick = { scope.launch { sheetState.show() } }
                                ) {
                                    Icon(Icons.Filled.Add, "Quick Capture")
                                }
                            }
                        }
                    ) { paddingValues ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.DailyDashboard.route,
                            modifier = Modifier.padding(paddingValues)
                        ) {
                            composable(Screen.DailyDashboard.route) {
                                DashboardScreen(
                                    tasks = tasks,
                                    onTaskComplete = { task -> viewModel.completeTask(task) }
                                )
                            }
                            composable(Screen.Settings.route) {
                                SettingsScreen()
                            }
                            composable(Screen.About.route) {
                                AboutScreen()
                            }
                        }
                    }
                }

                if (sheetState.isVisible) {
                    ModalBottomSheet(
                        onDismissRequest = { scope.launch { sheetState.hide() } },
                        sheetState = sheetState
                    ) {
                        QuickCaptureSheet(
                            onSave = { title, context, priority ->
                                viewModel.addTask(title, context, priority)
                                scope.launch { sheetState.hide() }
                            }
                        )
                    }
                }
            }
        }
    }
}
