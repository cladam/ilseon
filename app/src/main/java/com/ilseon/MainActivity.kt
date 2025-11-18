package com.ilseon

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ilseon.data.bluetooth.BluetoothChecker
import com.ilseon.data.task.TaskRepository
import com.ilseon.ui.components.NavigationDrawerHeader
import com.ilseon.ui.navigation.Screen
import com.ilseon.ui.screen.AboutScreen
import com.ilseon.ui.screen.AnalyticsScreen
import com.ilseon.ui.screen.CompletedTasksScreen
import com.ilseon.ui.screen.ContextManagementScreen
import com.ilseon.ui.screen.DashboardScreen
import com.ilseon.ui.screen.NotesScreen
import com.ilseon.ui.screen.QuickCaptureSheet
import com.ilseon.ui.screen.SettingsScreen
import com.ilseon.ui.theme.IlseonTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var taskRepository: TaskRepository

    private val viewModel: TaskViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        lifecycleScope.launch {
            taskRepository.rescheduleAllReminders()
        }

        setContent {
            IlseonTheme {
                val context = LocalContext.current
                var hasNotificationPermission by remember {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        mutableStateOf(
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        )
                    } else {
                        mutableStateOf(true)
                    }
                }
                var hasRecordAudioPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }
                var hasBluetoothConnectPermission by remember {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        mutableStateOf(
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) == PackageManager.PERMISSION_GRANTED
                        )
                    } else {
                        mutableStateOf(true)
                    }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { permissions ->
                        hasNotificationPermission = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: hasNotificationPermission
                        hasRecordAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: hasRecordAudioPermission
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            hasBluetoothConnectPermission = permissions[Manifest.permission.BLUETOOTH_CONNECT] ?: hasBluetoothConnectPermission
                        }
                    }
                )

                LaunchedEffect(Unit) {
                    val permissionsToRequest = mutableListOf<String>()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                    permissionLauncher.launch(permissionsToRequest.toTypedArray())
                }

                var showExactAlarmPermissionDialog by remember { mutableStateOf(false) }
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    LaunchedEffect(Unit) {
                        showExactAlarmPermissionDialog = true
                    }
                }

                if (showExactAlarmPermissionDialog) {
                    AlertDialog(
                        onDismissRequest = { showExactAlarmPermissionDialog = false },
                        title = { Text("Permission Required") },
                        text = { Text("To ensure alarms and reminders are sent on time, please grant the 'Alarms & reminders' permission.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showExactAlarmPermissionDialog = false
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also {
                                            it.data = Uri.fromParts("package", context.packageName, null)
                                            context.startActivity(it)
                                        }
                                    }
                                }
                            ) {
                                Text("Open Settings")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showExactAlarmPermissionDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val tasks by viewModel.tasks.collectAsState()
                val activeFocusBlock by viewModel.activeFocusBlock.collectAsState()
                var completedTaskIds by remember { mutableStateOf<Set<UUID>>(emptySet()) }
                var vttResult by remember { mutableStateOf("") }

                val speechRecognizerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val data: Intent? = result.data
                        val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        results?.firstOrNull()?.let { text ->
                            vttResult = text
                            scope.launch { sheetState.show() }
                        }
                    }
                }

                val bluetoothChecker = remember { BluetoothChecker(context) }

                val isRightHanded by remember { mutableStateOf(true) }

                val createFileLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("text/plain"),
                    onResult = { uri: Uri? ->
                        uri?.let {
                            settingsViewModel.exportNotes { exportedData ->
                                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                                    OutputStreamWriter(outputStream).use { writer ->
                                        writer.write(exportedData)
                                    }
                                }
                            }
                        }
                    }
                )

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            NavigationDrawerHeader(
                                onSettingsClick = {
                                    navController.navigate(Screen.Settings.route)
                                    scope.launch { drawerState.close() }
                                }
                            )
                            HorizontalDivider()
                            DrawerContent(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route)
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { },
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
                                LargeFloatingActionButton(
                                    onClick = {
                                        if (bluetoothChecker.isHeadsetConnected()) {
                                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                putExtra(
                                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                                )
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                                            }
                                            speechRecognizerLauncher.launch(intent)
                                        } else {
                                            scope.launch { sheetState.show() }
                                        }
                                    },
                                    shape = CircleShape,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    )
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Filled.Add,
                                            contentDescription = "Quick Capture"
                                        )
                                        Text(
                                            text = "QUICK CAPTTURE",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            fontSize = 10.sp
                                        )
                                    }
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
                                    completedTaskIds = completedTaskIds,
                                    onAnimateComplete = { task ->
                                        completedTaskIds = completedTaskIds + task.id
                                    },
                                    onTaskComplete = { task, reflection ->
                                        viewModel.completeTask(task, reflection)
                                    },
                                    onTaskTimerFinished = { task ->
                                        viewModel.onTaskTimerFinished(task)
                                    },
                                    onStartTask = { task ->
                                        viewModel.startTaskTimer(task)
                                    },
                                    onPauseTask = { task ->
                                        viewModel.pauseTaskTimer(task)
                                    },
                                    activeFocusBlock = activeFocusBlock,
                                )
                            }
                            composable(Screen.Notes.route) {
                                NotesScreen()
                            }
                            composable(Screen.Settings.route) {
                                SettingsScreen(
                                    onCompletedTasksClick = {
                                        navController.navigate("completed_tasks")
                                    },
                                    onAboutClick = {
                                        navController.navigate(Screen.About.route)
                                    },
                                    onExportClick = {
                                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                        val fileName = "ilseon_tasks_${dateFormat.format(Date())}.txt"
                                        createFileLauncher.launch(fileName)
                                    }
                                 )
                            }
                            composable(Screen.About.route) {
                                AboutScreen()
                            }
                            composable(Screen.Analytics.route) {
                                AnalyticsScreen(
                                    onNavigateToCompletedTasks = { navController.navigate("completed_tasks") }
                                )
                            }
                            composable(Screen.ContextManagement.route) {
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
                        onDismissRequest = {
                            vttResult = ""
                            scope.launch { sheetState.hide() }
                         },
                        sheetState = sheetState
                    ) {
                        QuickCaptureSheet(
                            onSave = { title, description, contextId, priority, startTime, endTime, duration ->
                                viewModel.addTask(title, description, contextId, priority, startTime, endTime, duration)
                                scope.launch { sheetState.hide() }
                                vttResult = ""
                            },
                            initialTitle = vttResult
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerContent(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val navigationItems = listOf(
        "Dashboard" to Screen.DailyDashboard.route,
        "Notes" to Screen.Notes.route,
        "Contexts" to Screen.ContextManagement.route,
        "Analytics" to Screen.Analytics.route
    )

    navigationItems.forEach { (title, route) ->
        NavigationDrawerItem(
            label = { Text(title) },
            selected = currentRoute == route,
            onClick = { onNavigate(route) },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = Color.DarkGray
            )
        )
    }
}