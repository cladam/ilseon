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
        setContent {
            IlseonTheme {
                CaptureTypeDialog(
                    onDismiss = { finish() },
                    onCaptureTypeSelected = { type ->
                        val intent = Intent(this, MainActivity::class.java).apply {
                            putExtra("capture_type", type)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                )
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

@Preview(showBackground = true)
@Composable
fun CaptureTypeDialogPreview() {
    IlseonTheme {
        CaptureTypeDialog(onDismiss = {}, onCaptureTypeSelected = {})
    }
}
