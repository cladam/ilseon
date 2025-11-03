package com.ilseon.ui.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.ilseon.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                NavigationDrawerItem(
                    label = { Text("Dashboard") },
                    selected = false,
                    onClick = {
                        navController.navigate(Screen.DailyDashboard.route)
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("About") },
                    selected = false,
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
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Screen.QuickCapture.route) },
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
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.DailyDashboard.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(Screen.DailyDashboard.route) { DailyDashboardScreen() }
                composable(Screen.QuickCapture.route) { QuickCaptureScreen() }
                composable(Screen.About.route) { AboutScreen() }
            }
        }
    }
}