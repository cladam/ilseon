package com.ilseon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ilseon.ui.navigation.Screen
import com.ilseon.ui.screen.AboutScreen
import com.ilseon.ui.screen.CompletedTasksScreen
import com.ilseon.ui.screen.ContextManagementScreen
import com.ilseon.ui.screen.DashboardScreen
import com.ilseon.ui.screen.QuickCaptureSheet
import com.ilseon.ui.screen.SettingsScreen
import com.ilseon.ui.theme.IlseonTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID

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
                var completedTaskIds by remember { mutableStateOf<Set<UUID>>(emptySet()) }

                // This would be loaded from user preferences
                val isRightHanded by remember { mutableStateOf(true) }

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
                        floatingActionButtonPosition = if (isRightHanded) FabPosition.End else FabPosition.Start,
                        floatingActionButton = {
                            if (currentRoute == Screen.DailyDashboard.route) {
                                ExtendedFloatingActionButton(
                                    onClick = { scope.launch { sheetState.show() } },
                                    icon = { Icon(Icons.Filled.Add, contentDescription = "Quick Capture") },
                                    text = { Text("Quick Capture") },
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary,
                                        FloatingActionButtonDefaults.shape
                                    )
                                )
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
                                    completedTaskIds = completedTaskIds,
                                    onAnimateComplete = { task ->
                                        completedTaskIds = completedTaskIds + task.id
                                    },
                                    onTaskComplete = { task ->
                                        viewModel.completeTask(task)
                                    },
                                )
                            }
                            composable(Screen.Settings.route) {
                                SettingsScreen(
                                    onManageContextsClick = {
                                        navController.navigate("context_management")
                                    },
                                    onCompletedTasksClick = {
                                        navController.navigate("completed_tasks")
                                    }
                                )
                            }
                            composable(Screen.About.route) {
                                AboutScreen()
                            }
                            composable("context_management") {
                                ContextManagementScreen()
                            }
                            composable("completed_tasks") {
                                CompletedTasksScreen()
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
                            onSave = { title, contextId, priority, startTime, endTime ->
                                viewModel.addTask(title, contextId, priority, startTime, endTime)
                                scope.launch { sheetState.hide() }
                            }
                        )
                    }
                }
            }
        }
    }
}
