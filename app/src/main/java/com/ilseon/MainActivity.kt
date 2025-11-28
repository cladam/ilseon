package com.ilseon

import android.Manifest
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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.ui.draw.alpha
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
import com.ilseon.ui.components.ReflectionDialog
import com.ilseon.ui.navigation.Screen
import com.ilseon.ui.screen.AboutScreen
import com.ilseon.ui.screen.AnalyticsScreen
import com.ilseon.ui.screen.ArchiveScreen
import com.ilseon.ui.screen.CompletedTasksScreen
import com.ilseon.ui.screen.ContextManagementScreen
import com.ilseon.ui.screen.DashboardScreen
import com.ilseon.ui.screen.IdeaInboxScreen
import com.ilseon.ui.screen.NextTaskActivationScreen
import com.ilseon.ui.screen.ReflectionScreen
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
    private val contextViewModel: TaskContextViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

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

                val createFileLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("text/plain"),
                    onResult = { uri: Uri? ->
                        uri?.let {
                            settingsViewModel.exportReflections { exportedData ->
                                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                                    OutputStreamWriter(outputStream).use { writer ->
                                        writer.write(exportedData)
                                    }
                                }
                            }
                        }
                    }
                )
                
                LaunchedEffect(intent) {
                    handleIntent(intent,
                        onShowTaskSheet = { scope.launch { sheetState.show() } },
                        onShowIdeaDialog = {
                            navController.navigate(Screen.IdeaInbox.route)
                            showAddIdeaDialog = true
                        }
                    )
                    // Clear the extra to avoid re-triggering
                    intent?.removeExtra("capture_type")
                }

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
                                },
                                actions = {
                                    StreakIndicator(streak = completionStreak)
                                }
                            )
                        },
                        floatingActionButtonPosition = if (isRightHanded) FabPosition.End else FabPosition.Start,
                        floatingActionButton = {
                            if (currentRoute == Screen.DailyDashboard.route || currentRoute == Screen.IdeaInbox.route) {
                                LargeFloatingActionButton(
                                    onClick = {
                                        val useVtt = bluetoothSstEnabled && bluetoothChecker.isHeadsetConnected()
                                        if (currentRoute == Screen.IdeaInbox.route) {
                                            vttTarget = "idea_content"
                                            if (useVtt) {
                                                startVtt()
                                            } else {
                                                vttIdeaContentResult = ""
                                                showAddIdeaDialog = true
                                            }
                                        } else { // Dashboard
                                            vttTarget = "quick_capture_title"
                                            if (useVtt) {
                                                startVtt()
                                            } else {
                                                scope.launch { sheetState.show() }
                                            }
                                        }
                                    },
                                    shape = CircleShape,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .size(112.dp)
                                        .border(
                                            3.dp,
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
                                            text = "QUICK CAPTURE",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            fontSize = 12.sp
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
                                    ReflectionDialog(
                                        taskTitle = data.task.title,
                                        phonePickups = data.phonePickups,
                                        onSave = { reflection ->
                                            viewModel.completeTask(data.task, reflection)
                                        },
                                        onDismiss = {
                                            viewModel.onReflectionDialogDismiss()
                                        }
                                    )
                                }

                                DashboardScreen(
                                    tasks = tasks,
                                    completedTaskIds = completedTaskIds,
                                    onAnimateComplete = { task ->
                                        completedTaskIds = completedTaskIds + task.id
                                    },
                                    onTaskComplete = { task ->
                                        viewModel.onShowReflectionDialog(task.id)
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
                                    },
                                    onArchiveClick = {
                                        navController.navigate("archive_tasks")
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
                                    initialContextDescription = vttContextDescriptionResult
                                )
                            }
                            composable(Screen.IdeaInbox.route) {
                                IdeaInboxScreen(
                                    onNavigateToNewTask = { title, description ->
                                        vttTitleResult = title
                                        vttDescriptionResult = description
                                        onTaskSavedFromIdea = true
                                        scope.launch { sheetState.show() }
                                    },
                                    showAddIdeaDialog = showAddIdeaDialog,
                                    onDismissAddIdeaDialog = {
                                        showAddIdeaDialog = false
                                        vttIdeaContentResult = "" // Reset after dialog dismiss
                                    },
                                    vttIdeaContent = vttIdeaContentResult,
                                    onVttClick = {
                                        vttTarget = "idea_content"
                                        startVtt()
                                    }
                                )
                            }
                            composable("completed_tasks") {
                                CompletedTasksScreen()
                            }
                            composable("archive_tasks") {
                                ArchiveScreen()
                            }
                        }
                    }
                }

                if (sheetState.isVisible) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            vttTitleResult = ""
                            vttDescriptionResult = ""
                            scope.launch { sheetState.hide() }
                        },
                        sheetState = sheetState,
                        windowInsets = WindowInsets.ime
                    ) {
                        QuickCaptureSheet(
                            onSave = { title, description, contextId, priority, startTime, endTime, duration, isRecurring, recurrenceDays, isForTomorrow ->
                                viewModel.addTask(
                                    title,
                                    description,
                                    contextId,
                                    priority,
                                    startTime,
                                    endTime,
                                    duration,
                                    isRecurring,
                                    recurrenceDays,
                                    isForTomorrow
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
        handleIntent(intent,
            onShowTaskSheet = { /* We need a coroutine scope here, but can't get one. Will be handled by LaunchedEffect. */ },
            onShowIdeaDialog = { /* Same as above. */ }
        )
    }

    private fun handleIntent(intent: Intent?, onShowTaskSheet: () -> Unit, onShowIdeaDialog: () -> Unit) {
        if (intent == null) return
        when (intent.getStringExtra("capture_type")) {
            "task" -> onShowTaskSheet()
            "idea" -> onShowIdeaDialog()
        }
        
        if (intent.action == "com.ilseon.ACTION_SHOW_REFLECTION") {
            val taskIdString = intent.getStringExtra("EXTRA_TASK_ID")
            if (taskIdString != null) {
                viewModel.onShowReflectionDialog(UUID.fromString(taskIdString))
            }
        }
    }
}

@Composable
fun StreakIndicator(streak: Int) {
    val MutedGold = Color(0xFFC9B464)

    Box(
        modifier = Modifier
            .padding(end = 16.dp)
            .size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            streak >= 7 -> { // Mastery Badge
                Icon(
                    imageVector = Icons.Filled.WorkspacePremium,
                    contentDescription = "Mastery Badge: $streak",
                    tint = MutedGold,
                    modifier = Modifier.size(24.dp)
                )
            }
            streak >= 5 -> { // Deep Focus - Subtle Alpha Pulse (Breathing Effect)
                val infiniteTransition = rememberInfiniteTransition(label = "streak-pulse")
                
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.7f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "streak-pulse-alpha"
                )

                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Deep Focus Streak: $streak",
                    tint = MutedGold,
                    modifier = Modifier.alpha(pulseAlpha)
                )
            }
            streak >= 3 -> { // Momentum
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Momentum Streak: $streak",
                    tint = MutedGold
                )
            }
            streak >= 1 -> { // Initiation
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MutedGold, CircleShape)
                )
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
        "Idea Inbox" to Screen.IdeaInbox.route,
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
                selectedContainerColor = Color.DarkGray
            )
        )
    }
}