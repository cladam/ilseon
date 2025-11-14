package com.ilseon.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun ReflectionDialog(
    taskTitle: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var reflectionText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How did \"$taskTitle\" go?") },
        text = {
            OutlinedTextField(
                value = reflectionText,
                onValueChange = { reflectionText = it },
                label = { Text("Your reflection...") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = {
                onSave(reflectionText)
            }) {
                Text("Save & Close Focus")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}