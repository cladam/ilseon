package com.ilseon

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ilseon.data.task.TaskRepository
import com.ilseon.service.HapticManager
import com.ilseon.ui.theme.IlseonTheme
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ActionTrampolineActivity : ComponentActivity() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ActionTrampolineEntryPoint {
        fun taskRepository(): TaskRepository
        fun hapticManager(): HapticManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.action == "com.ilseon.action.TRIGGER_FOLLOW_UP") {
            handleTriggerFollowUp()
            finish()
            return
        }

        val targetIntent = when (intent.action) {
            "com.ilseon.action.NEW_TASK" -> Intent(this, MainActivity::class.java).apply {
                putExtra("capture_type", "task")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            "com.ilseon.action.NEW_IDEA" -> Intent(this, MainActivity::class.java).apply {
                putExtra("capture_type", "idea")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            "com.ilseon.action.NEW_VOICE_MEMO" -> Intent(this, MainActivity::class.java).apply {
                putExtra("navigate_to", "voice_recorder")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            else -> null
        }

        targetIntent?.let { startActivity(it) }
        finish()
    }

    private fun handleTriggerFollowUp() {
        Log.d("ActionTrampoline", "Received TRIGGER_FOLLOW_UP from widget")
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ActionTrampolineEntryPoint::class.java
        )

        try {
            entryPoint.hapticManager().performNudge()
        } catch (e: Exception) {
            Log.w("ActionTrampoline", "Failed to perform haptic nudge", e)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = entryPoint.taskRepository().createQuickFollowUpTask()
                Log.d("ActionTrampoline", "Created quick follow-up task: ${task.id} (${task.title})")
            } catch (e: Exception) {
                Log.e("ActionTrampoline", "Failed to create quick follow-up task", e)
            }
        }
    }
}


@Composable
fun CaptureTypeDialog(
    onDismiss: () -> Unit,
    onCaptureTypeSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Capture") },
        text = { Text("What would you like to capture?") },
        confirmButton = {
            Button(
                onClick = { onCaptureTypeSelected("task") }
            ) {
                Text("Task")
            }
        },
        dismissButton = {
            Button(
                onClick = { onCaptureTypeSelected("idea") }
            ) {
                Text("Idea")
            }
        }
    )
}