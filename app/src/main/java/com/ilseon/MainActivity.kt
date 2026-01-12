package com.ilseon

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Battery1Bar
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.ilseon.data.EnergyLevel
import com.ilseon.data.bluetooth.BluetoothChecker
import com.ilseon.data.task.TaskRepository
import com.ilseon.data.toColor
import com.ilseon.notifications.FuelCheckScheduler
import com.ilseon.ui.onboarding.OnboardingManager
import com.ilseon.ui.onboarding.OnboardingScreen
import com.ilseon.ui.components.NavigationDrawerHeader
import com.ilseon.ui.components.ReflectionDialog
import com.ilseon.ui.components.StreakIndicator
import com.ilseon.ui.navigation.Screen
import com.ilseon.ui.screen.AboutScreen
import com.ilseon.ui.screen.AnalyticsScreen
import com.ilseon.ui.screen.ArchiveScreen
import com.ilseon.ui.screen.CompletedTasksScreen
import com.ilseon.ui.screen.ContextManagementScreen
import com.ilseon.ui.screen.DashboardScreen
import com.ilseon.ui.screen.FuelCheckScreen
import com.ilseon.ui.screen.IdeaInboxScreen
import com.ilseon.ui.screen.NextTaskActivationScreen
import com.ilseon.ui.screen.OngoingTasksScreen
import com.ilseon.ui.screen.ReflectionScreen
import com.ilseon.ui.screen.QuickCaptureSheet
import com.ilseon.ui.screen.RecorderScreen
import com.ilseon.ui.screen.SettingsScreen
import com.ilseon.ui.screen.VoiceInboxScreen
import com.ilseon.ui.theme.BorderQuiet
import com.ilseon.ui.theme.CustomTextPrimary
import com.ilseon.ui.theme.IlseonTheme
import com.ilseon.ui.theme.LightGrey
import com.ilseon.ui.theme.MutedDetail
import com.ilseon.ui.theme.MutedRed
import com.ilseon.ui.theme.MutedTeal
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.compareTo
import kotlin.random.Random

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var fuelCheckScheduler: FuelCheckScheduler

    @Inject
    lateinit var onboardingManager: OnboardingManager

    private val viewModel: TaskViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val contextViewModel: TaskContextViewModel by viewModels()

    private val intentState = mutableStateOf<Intent?>(null)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intentState.value = intent
        installSplashScreen()

        lifecycleScope.launch {
            taskRepository.rescheduleAllReminders()
        }

        if (onboardingManager.isOnboardingCompleted()) {
            fuelCheckScheduler.scheduleNextFuelCheck()
        }

        setContent {
            IlseonTheme {
                val fuelCheckViewModel: FuelCheckViewModel = hiltViewModel()
                val userStatus by fuelCheckViewModel.userStatus.collectAsState()
                LaunchedEffect(userStatus) {
                    Log.d("FuelCheck", "userStatus: $userStatus, energy: ${userStatus?.currentEnergy}")
                }
                val context = LocalContext.current
                var onboardingCompleted by remember { mutableStateOf(onboardingManager.isOnboardingCompleted()) }

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

                var showExactAlarmPermissionDialog by remember { mutableStateOf(false) }
                val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager

                LaunchedEffect(onboardingCompleted) {
                    if (onboardingCompleted) {
                        val permissionsToRequest = mutableListOf<String>()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
                        }
                        permissionLauncher.launch(permissionsToRequest.toTypedArray())

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                            showExactAlarmPermissionDialog = true
                        }
                    }
                }

                if (showExactAlarmPermissionDialog) {
                    AlertDialog(
                        onDismissRequest = { showExactAlarmPermissionDialog = false },
                        title = { Text("Permission Required") },
                        text = { Text("To ensure alarms and reminders are sent on time, please grant the \'Alarms & reminders\' permission.") },
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
                var showAddIdeaDialog by remember { mutableStateOf(false) }
                var onTaskSavedFromIdea by remember { mutableStateOf(false) }


                val tasks by viewModel.tasks.collectAsState()
                val completionStreak by viewModel.completionStreak.collectAsState()
                val activeFocusBlock by viewModel.activeFocusBlock.collectAsState()
                var completedTaskIds by remember { mutableStateOf<Set<UUID>>(emptySet()) }

                var vttTitleResult by remember { mutableStateOf("") }
                var vttDescriptionResult by remember { mutableStateOf("") }
                var vttIdeaContentResult by remember { mutableStateOf("") }
                var vttContextNameResult by remember { mutableStateOf("") }
                var vttContextDescriptionResult by remember { mutableStateOf("") }
                var vttTarget by remember { mutableStateOf("quick_capture_title") }

                var showFuelCheckOnStartup by remember { mutableStateOf(false) }

                val speechRecognizerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK) {
                        val data: Intent? = result.data
                        val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        results?.firstOrNull()?.let { text ->
                            when (vttTarget) {
                                "quick_capture_title" -> {
                                    vttTitleResult = text
                                    scope.launch { sheetState.show() }
                                }
                                "quick_capture_description" -> {
                                    vttDescriptionResult = text
                                    scope.launch { sheetState.show() }
                                }
                                "idea_content" -> {
                                    vttIdeaContentResult = text
                                    showAddIdeaDialog = true
                                }
                                "context_name" -> vttContextNameResult = text
                                "context_description" -> vttContextDescriptionResult = text
                            }
                        }
                    }
                }

                val bluetoothChecker = remember { BluetoothChecker(context) }
                val bluetoothSstEnabled by settingsViewModel.bluetoothSstEnabled.collectAsState()
                val sstLanguage by settingsViewModel.sstLanguage.collectAsState()

                val startVtt = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, sstLanguage)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                    }
                    speechRecognizerLauncher.launch(intent)
                }

                val isRightHanded by remember { mutableStateOf(true) }

                val intentToShow by remember { intentState }
                LaunchedEffect(intentToShow) {
                    intentToShow?.let { currentIntent ->
                        if (currentIntent.action == Intent.ACTION_VIEW && currentIntent.data != null) {
                            navController.handleDeepLink(currentIntent)
                        } else {
                            // Handle other custom intents
                            when (currentIntent.getStringExtra("capture_type")) {
                                "task" -> scope.launch { sheetState.show() }
                                "idea" -> {
                                    navController.navigate(Screen.IdeaInbox.route)
                                    showAddIdeaDialog = true
                                }
                            }
                            when (currentIntent.getStringExtra("navigate_to")) {
                                "voice_recorder" -> navController.navigate(Screen.Recorder.route)
                            }
                            if (currentIntent.getStringExtra("destination") == "fuel_check") {
                                navController.navigate(Screen.FuelCheck.route)
                            }
                            if (currentIntent.action == "com.ilseon.ACTION_SHOW_REFLECTION") {
                                val taskIdString = currentIntent.getStringExtra("EXTRA_TASK_ID")
                                if (taskIdString != null) {
                                    viewModel.onShowReflectionDialog(UUID.fromString(taskIdString))
                                }
                            }
                        }
                        // Clear intent after handling to prevent re-triggering
                        intentState.value = null
                    }
                }

                LaunchedEffect(Unit) {
                    if (onboardingManager.isOnboardingCompleted() && Random.nextFloat() < 0.20f) {
                        showFuelCheckOnStartup = true
                    }
                }

                LaunchedEffect(showFuelCheckOnStartup) {
                    if (showFuelCheckOnStartup) {
                        navController.navigate(Screen.FuelCheck.route)
                        showFuelCheckOnStartup = false
                    }
                }

                val startDestination = if (onboardingCompleted) Screen.DailyDashboard.route else Screen.Onboarding.route

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
                            if (currentRoute != Screen.Recorder.route) {
                                TopAppBar(
                                    title = { },
                                    navigationIcon = {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                                        }
                                    },
                                    actions = {
                                        val energyLevel = userStatus?.currentEnergy
                                        val icon = when (energyLevel) {
                                            EnergyLevel.High -> Icons.Filled.BatteryFull
                                            EnergyLevel.Medium -> Icons.Filled.Battery3Bar
                                            EnergyLevel.Low -> Icons.Filled.Battery1Bar
                                            else -> Icons.Filled.BatteryChargingFull
                                        }
                                        val tint = energyLevel?.toColor() ?: MaterialTheme.colorScheme.onSurfaceVariant

                                        IconButton(onClick = { navController.navigate(Screen.FuelCheck.route) }) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = "Fuel Check",
                                                tint = tint
                                            )
                                        }
                                        StreakIndicator(
                                            streak = completionStreak,
                                            modifier = Modifier.padding(end = 16.dp).size(24.dp)
                                        )
                                    }
                                )
                            }
                        },
                        floatingActionButtonPosition = if (isRightHanded) FabPosition.End else FabPosition.Start,
                        floatingActionButton = {
                            val isVoiceInbox = currentRoute?.startsWith(Screen.VoiceInbox.route) == true
                            val isIdeaInbox = currentRoute?.startsWith(Screen.IdeaInbox.route) == true
                            if (currentRoute == Screen.DailyDashboard.route || isIdeaInbox || isVoiceInbox) {
                                Box(
                                    modifier = Modifier
                                        .size(118.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                                        .border(1.dp, MutedDetail, CircleShape)
                                        .clickable {
                                            val useVtt = bluetoothSstEnabled && bluetoothChecker.isHeadsetConnected()
                                            when {
                                                isVoiceInbox -> navController.navigate(Screen.Recorder.route)
                                                isIdeaInbox -> {
                                                    vttTarget = "idea_content"
                                                    if (useVtt) startVtt() else {
                                                        vttIdeaContentResult = ""
                                                        showAddIdeaDialog = true
                                                    }
                                                }
                                                else -> {
                                                    vttTarget = "quick_capture_title"
                                                    if (useVtt) startVtt() else scope.launch { sheetState.expand() }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        if (isVoiceInbox) {
                                            Icon(
                                                Icons.Filled.Mic,
                                                contentDescription = "Record",
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Text(
                                                text = "RECORD",
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                fontSize = 12.sp
                                            )
                                        } else {
                                            Icon(
                                                Icons.Filled.Add,
                                                contentDescription = "Quick Capture",
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Text(
                                                text = "QUICK CAPTURE",
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    ) { paddingValues ->
                        NavHost(
                            navController = navController,
                            startDestination = startDestination,
                            modifier = Modifier.padding(paddingValues)
                        ) {
                            composable(Screen.Onboarding.route) {
                                OnboardingScreen(onOnboardingFinished = {
                                    onboardingManager.setOnboardingCompleted()
                                    onboardingCompleted = true
                                    fuelCheckScheduler.scheduleNextFuelCheck()
                                    navController.navigate(Screen.DailyDashboard.route) {
                                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                                    }
                                })
                            }
                            composable(Screen.DailyDashboard.route) {
                                val reflectionData by viewModel.taskForReflection.collectAsState()
                                val postCompletionAction by viewModel.postCompletionAction.collectAsState()

                                LaunchedEffect(postCompletionAction) {
                                    when(postCompletionAction) {
                                        is PostCompletionAction.ActivateNextTask -> {
                                            viewModel.onReflectionDialogDismiss()
                                            navController.navigate(Screen.NextTaskActivation.route)
                                        }
                                        is PostCompletionAction.GoToDashboard -> {
                                            viewModel.onReflectionDialogDismiss()
                                            viewModel.postCompletionActionHandled()
                                        }
                                        is PostCompletionAction.Idle -> { /* Do nothing */ }
                                    }
                                }

                                reflectionData?.let { data ->
                                    val context = LocalContext.current
                                    val reduceMotion = remember {
                                        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
                                        accessibilityManager.isEnabled &&
                                                Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
                                    }

                                    ReflectionDialog(
                                        taskTitle = data.task.title,
                                        phonePickups = data.phonePickups,
                                        onSave = { reflection, energyLevel ->
                                            completedTaskIds = completedTaskIds - data.task.id
                                            viewModel.completeTask(data.task, reflection, energyLevel)

                                            if (Random.nextFloat() < 0.30f) {
                                                navController.navigate(Screen.FuelCheck.route)
                                            }
                                        },
                                        onDismiss = {
                                            completedTaskIds = completedTaskIds - data.task.id
                                            viewModel.onReflectionDialogDismiss()
                                        },
                                        reduceMotion = reduceMotion
                                    )
                                }

                                DashboardScreen(
                                    tasks = tasks,
                                    completedTaskIds = completedTaskIds,
                                    onAnimateComplete = { task ->
                                        completedTaskIds = completedTaskIds + task.id
                                        viewModel.onShowReflectionDialog(task.id)
                                    },
                                    onTaskComplete = { task ->
                                        completedTaskIds = completedTaskIds + task.id
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
                                    onSwipeUp = {
                                        scope.launch { sheetState.show() }
                                    }
                                )
                            }
                            composable(Screen.NextTaskActivation.route) {
                                val action = viewModel.postCompletionAction.collectAsState().value
                                if (action is PostCompletionAction.ActivateNextTask) {
                                    val contextsWithFocusBlock by contextViewModel.contextsWithFocusBlock.collectAsState()
                                    val contextMap = remember(contextsWithFocusBlock) {
                                        contextsWithFocusBlock.associate { it.context.id to it.context }
                                    }
                                    NextTaskActivationScreen(
                                        nextTask = action.task,
                                        contextMap = contextMap,
                                        onStartNextBlock = {
                                            viewModel.startNextTask(action.task)
                                            navController.popBackStack()
                                        },
                                        onGoToFilter = {
                                            viewModel.postCompletionActionHandled()
                                            navController.popBackStack()
                                        }
                                    )
                                 }
                            }
                            composable(Screen.Reflections.route) {
                                ReflectionScreen()
                            }
                            composable(Screen.Settings.route) {
                                SettingsScreen(
                                    onCompletedTasksClick = { navController.navigate("completed_tasks") },
                                    onAboutClick = { navController.navigate(Screen.About.route) },
                                    onArchiveClick = { navController.navigate("archive_tasks") },
                                    onNavigateToIdeaInbox = { navController.navigate(Screen.IdeaInbox.route) },
                                    onNavigateToReflections = { navController.navigate(Screen.Reflections.route) }
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
                                ContextManagementScreen(
                                    onNewContextVttClick = {
                                        vttTarget = "context_name"
                                        startVtt()
                                    },
                                    onNewContextDescriptionVttClick = {
                                        vttTarget = "context_description"
                                        startVtt()
                                    },
                                    initialContextName = vttContextNameResult,
                                    initialContextDescription = vttContextDescriptionResult,
                                    onNavigateToOngoingTasks = { contextId ->
                                        navController.navigate("ongoing_tasks/$contextId")
                                    }
                                )
                            }
                            composable(
                                route = "${Screen.IdeaInbox.route}?ideaId={ideaId}",
                                arguments = listOf(navArgument("ideaId") { nullable = true })
                            ) { backStackEntry ->
                                IdeaInboxScreen(
                                    onNavigateToNewTask = { title, description ->
                                        vttTitleResult = title
                                        vttDescriptionResult = description
                                        onTaskSavedFromIdea = true
                                        scope.launch { sheetState.show() }
                                    },
                                    onNavigateToDashboard = { navController.navigate(Screen.DailyDashboard.route) },
                                    showAddIdeaDialog = showAddIdeaDialog,
                                    onDismissAddIdeaDialog = {
                                        showAddIdeaDialog = false
                                        vttIdeaContentResult = ""
                                    },
                                    vttIdeaContent = vttIdeaContentResult,
                                    onVttClick = {
                                        vttTarget = "idea_content"
                                        startVtt()
                                    },
                                    onSwipeUp = { showAddIdeaDialog = true },
                                    newIdeaId = backStackEntry.arguments?.getString("ideaId")?.let { UUID.fromString(it) }
                                )
                            }
                            composable(Screen.FuelCheck.route) {
                                FuelCheckScreen(onNavigateBack = { navController.popBackStack() })
                            }
                            composable(
                                route = "${Screen.VoiceInbox.route}?memoId={memoId}",
                                arguments = listOf(navArgument("memoId") { nullable = true }),
                                deepLinks = listOf(navDeepLink { uriPattern = "ilseon://play-voice-memo/{memoId}" })
                            ) { backStackEntry ->
                                VoiceInboxScreen(
                                    onNavigateToNewTask = { title, description ->
                                        vttTitleResult = title
                                        vttDescriptionResult = description
                                        onTaskSavedFromIdea = true
                                        scope.launch { sheetState.show() }
                                    },
                                    initialMemoIdToPlay = backStackEntry.arguments?.getString("memoId"),
                                    onNavigateToIdea = { ideaId ->
                                        navController.navigate("${Screen.IdeaInbox.route}?ideaId=$ideaId")
                                    },
                                    onNavigateToDashboard = {
                                        navController.navigate(Screen.DailyDashboard.route) {
                                            popUpTo(Screen.VoiceInbox.route) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable(Screen.Recorder.route) {
                                val recorderViewModel: RecorderViewModel = hiltViewModel()
                                val voiceMemoViewModel: VoiceMemoViewModel = hiltViewModel()
                                val recorderState by recorderViewModel.uiState.collectAsState()
                                val duration by recorderViewModel.durationSeconds.collectAsState()

                                LaunchedEffect(Unit) {
                                    recorderViewModel.startRecording()
                                }

                                RecorderScreen(
                                    recorderState = recorderState,
                                    durationSeconds = duration,
                                    onStartRecording = { recorderViewModel.startRecording() },
                                    onPauseRecording = { recorderViewModel.pauseRecording() },
                                    onResumeRecording = { recorderViewModel.resumeRecording() },
                                    onStopRecording = {
                                        val result = recorderViewModel.getRecordingResult() ?: recorderViewModel.stopRecording()
                                        result?.let {
                                            voiceMemoViewModel.saveVoiceMemo(
                                                filePath = it.filePath,
                                                durationSeconds = it.durationSeconds,
                                                onComplete = {
                                                    navController.navigate(Screen.VoiceInbox.route) {
                                                        popUpTo(Screen.Recorder.route) { inclusive = true }
                                                    }
                                                }
                                            )
                                        } ?: navController.navigate(Screen.VoiceInbox.route) {
                                            popUpTo(Screen.Recorder.route) { inclusive = true }
                                        }
                                    },
                                    onCancel = {
                                        recorderViewModel.discardRecording()
                                        navController.popBackStack()
                                    },
                                    onSave = {
                                        Log.d("RecorderScreen", "onSave called")
                                        val result = recorderViewModel.getRecordingResult() ?: recorderViewModel.stopRecording()
                                        Log.d("RecorderScreen", "onSave result: $result")
                                        result?.let {
                                            voiceMemoViewModel.saveVoiceMemo(
                                                filePath = it.filePath,
                                                durationSeconds = it.durationSeconds,
                                                onComplete = {
                                                    navController.navigate(Screen.VoiceInbox.route) {
                                                        popUpTo(Screen.Recorder.route) { inclusive = true }
                                                    }
                                                }
                                            )
                                        } ?: navController.navigate(Screen.VoiceInbox.route) {
                                            popUpTo(Screen.Recorder.route) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("completed_tasks") {
                                CompletedTasksScreen()
                            }
                            composable("archive_tasks") {
                                ArchiveScreen()
                            }
                            composable("ongoing_tasks/{contextId}") { backStackEntry ->
                                OngoingTasksScreen(contextId = backStackEntry.arguments?.getString("contextId"))
                            }
                        }
                    }
                }

                if (sheetState.isVisible) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            if (sheetState.currentValue != SheetValue.Expanded) {
                                vttTitleResult = ""
                                vttDescriptionResult = ""
                                scope.launch { sheetState.hide() }
                            }
                        },
                        sheetState = sheetState,
                        properties = ModalBottomSheetProperties(
                            shouldDismissOnBackPress = true
                        )
                    ) {
                        QuickCaptureSheet(
                            sheetState = sheetState,
                            onSave = { title, description, contextId, priority, isUrgent, startTime, endTime, duration, isRecurring, recurrenceDays, isForTomorrow, energyLevel ->
                                viewModel.addTask(
                                    title,
                                    description,
                                    contextId,
                                    priority,
                                    isUrgent,
                                    startTime,
                                    endTime,
                                    duration,
                                    isRecurring,
                                    recurrenceDays,
                                    isForTomorrow,
                                    energyLevel
                                )
                                scope.launch { sheetState.hide() }.invokeOnCompletion { 
                                    if (onTaskSavedFromIdea) {
                                        navController.navigate(Screen.DailyDashboard.route) {
                                            popUpTo(navController.graph.startDestinationId)
                                        }
                                        onTaskSavedFromIdea = false
                                    }
                                }
                                vttTitleResult = ""
                                vttDescriptionResult = ""
                            },
                            initialTitle = vttTitleResult,
                            initialDescription = vttDescriptionResult,
                            onTitleVttClick = {
                                vttTarget = "quick_capture_title"
                                startVtt()
                            },
                            onDescriptionVttClick = {
                                vttTarget = "quick_capture_description"
                                startVtt()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intentState.value = intent
    }
}

@Composable
private fun DrawerContent(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val navigationItems = listOf(
        "Dashboard" to Screen.DailyDashboard.route,
        "Idea Inbox" to Screen.IdeaInbox.route,
        "Voice Inbox" to Screen.VoiceInbox.route,
        "Reflections" to Screen.Reflections.route,
        "Contexts" to Screen.ContextManagement.route,
        "Analytics" to Screen.Analytics.route
    )

    navigationItems.forEach { (title, route) ->
        NavigationDrawerItem(
            label = { Text(title) },
            selected = currentRoute == route,
            onClick = { onNavigate(route) },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = androidx.compose.ui.graphics.Color.DarkGray
            )
        )
    }
}
