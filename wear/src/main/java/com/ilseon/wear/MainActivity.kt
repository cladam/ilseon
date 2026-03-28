package com.ilseon.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.ilseon.wear.tile.WearTaskData
import com.ilseon.wear.tile.WearTaskDataLoader
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PriorityTaskScreen()
            }
        }
    }
}

// --- Ilseon palette (matches tile & widget) ---
private val ColorTeal = Color(0xFF5A9B80)
private val ColorRed = Color(0xFFB35F5F)
private val ColorTextPrimary = Color(0xFFE0E0E0)
private val ColorTextSecondary = Color(0xFF9E9E9E)
private val ColorButtonBg = Color(0xFF1E1E1E)
private val ColorRecording = Color(0xFFEF5350)

@Composable
fun PriorityTaskScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var task by remember { mutableStateOf<WearTaskData?>(null) }
    val isRecording by WearTaskDataLoader.recordingState.collectAsState()
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        task = WearTaskDataLoader.loadTaskData(context)
        WearTaskDataLoader.loadRecordingState(context) // seeds the shared flow
        isLoading = false
    }

    val listState = rememberScalingLazyListState()

    Scaffold(
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
        timeText = { TimeText() }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // App header
            item {
                Text(
                    text = "Ilseon",
                    color = ColorTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            if (isLoading) {
                item {
                    Text(text = "Loading…", color = ColorTextSecondary, fontSize = 14.sp)
                }
            } else if (task != null) {
                // Title + divider as one item
                item { TaskHeader(task!!) }
                // Each description line as its own scrollable item
                val lines = task!!.description
                    ?.lines()
                    ?.filter { it.isNotBlank() }
                    ?: emptyList()
                items(lines.size) { index ->
                    Text(
                        text = lines[index],
                        color = ColorTextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }
            } else {
                item { EmptySection() }
            }

            // Voice recording button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                RecordingButton(
                    isRecording = isRecording,
                    onToggle = {
                        scope.launch {
                            WearTaskDataLoader.toggleRecording(context)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TaskHeader(task: WearTaskData) {
    val dividerColor = if (task.isOverdue) ColorRed else ColorTeal

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Title with urgency indicator
        Text(
            text = if (task.isUrgent) "🔥 ${task.title}" else task.title,
            color = ColorTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Colored divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(dividerColor)
        )
    }
}

@Composable
private fun EmptySection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "No priority task",
            color = ColorTextPrimary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Open Ilseon on your phone\nto set a priority",
            color = ColorTextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RecordingButton(
    isRecording: Boolean,
    onToggle: () -> Unit
) {
    val bgColor = if (isRecording) ColorRecording else ColorButtonBg
    val tintColor = if (isRecording) Color.White else ColorTeal
    val icon = if (isRecording) R.drawable.ic_outline_mic_24 else R.drawable.ic_outline_mic_24
    val label = if (isRecording) "Recording…" else "Voice Memo"

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.wear.compose.material.Button(
            onClick = onToggle,
            modifier = Modifier.size(48.dp),
            colors = androidx.wear.compose.material.ButtonDefaults.buttonColors(
                backgroundColor = bgColor
            )
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = label,
                tint = tintColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isRecording) ColorRecording else ColorTextSecondary,
            fontSize = 11.sp
        )
    }
}
