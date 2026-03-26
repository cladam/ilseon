package com.ilseon.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.wear.compose.material.Text
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
private val ColorAmber = Color(0xFFC08A3E)
private val ColorTextPrimary = Color(0xFFE0E0E0)
private val ColorTextSecondary = Color(0xFF9E9E9E)
private val ColorButtonBg = Color(0xFF1E1E1E)

@Composable
fun PriorityTaskScreen() {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var task by remember { mutableStateOf<WearTaskData?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        task = WearTaskDataLoader.loadTaskData(context)
        isLoading = false
    }

    val listState = rememberScalingLazyListState()

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
                Text(
                    text = "Loading…",
                    color = ColorTextSecondary,
                    fontSize = 14.sp
                )
            }
        } else if (task != null) {
            // Task content
            item { TaskSection(task!!) }
        } else {
            // Empty state
            item { EmptySection() }
        }

        // Action buttons
        item {
            Spacer(modifier = Modifier.height(8.dp))
            ActionButtonRow(
                onNewTask = { scope.launch { WearTaskDataLoader.sendAction(context, WearTaskData.ACTION_NEW_TASK) } },
                onNewIdea = { scope.launch { WearTaskDataLoader.sendAction(context, WearTaskData.ACTION_NEW_IDEA) } },
                onNewVoiceMemo = { scope.launch { WearTaskDataLoader.sendAction(context, WearTaskData.ACTION_NEW_VOICE_MEMO) } }
            )
        }
    }
}

@Composable
private fun TaskSection(task: WearTaskData) {
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

        Spacer(modifier = Modifier.height(6.dp))

        // Description
        task.description?.let {
            Text(
                text = it,
                color = ColorTextSecondary,
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
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
            text = "Use the buttons below\nto capture something",
            color = ColorTextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ActionButtonRow(
    onNewTask: () -> Unit,
    onNewIdea: () -> Unit,
    onNewVoiceMemo: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        ActionButton(R.drawable.ic_outline_add_task_24, "New Task", ColorRed, onNewTask)
        Spacer(modifier = Modifier.width(12.dp))
        ActionButton(R.drawable.ic_outline_lightbulb_24, "New Idea", ColorAmber, onNewIdea)
        Spacer(modifier = Modifier.width(12.dp))
        ActionButton(R.drawable.ic_outline_mic_24, "Voice Memo", ColorTeal, onNewVoiceMemo)
    }
}

@Composable
private fun ActionButton(
    iconRes: Int,
    contentDescription: String,
    tintColor: Color,
    onClick: () -> Unit
) {
    androidx.wear.compose.material.Button(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        colors = androidx.wear.compose.material.ButtonDefaults.buttonColors(
            backgroundColor = ColorButtonBg
        )
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = tintColor,
            modifier = Modifier.size(22.dp)
        )
    }
}
