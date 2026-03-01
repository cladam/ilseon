package com.ilseon

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ilseon.ui.theme.IlseonTheme

class ActionTrampolineActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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