package com.ilseon.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.ilseon.data.EnergyLevel
import com.ilseon.data.toColor
import kotlinx.coroutines.delay

@Composable
fun ReflectionDialog(
    taskTitle: String,
    phonePickups: Int?,
    onDismiss: () -> Unit,
    onSave: (String, EnergyLevel) -> Unit,
    reduceMotion: Boolean
) {
    var reflectionText by remember { mutableStateOf("") }
    var selectedEnergyLevel by remember { mutableStateOf(EnergyLevel.Medium) }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(if (reduceMotion) 100 else 350)
        showDialog = true
    }

    val scale by animateFloatAsState(
        targetValue = if (showDialog) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = ""
    )

    val alpha by animateFloatAsState(
        targetValue = if (showDialog) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = ""
    )

    if (showDialog) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
        ) {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("How did \"$taskTitle\" go?") },
                text = {
                    Column {
                        phonePickups?.let {
                            if (it >= 0) {
                                Text(
                                    text = "During your timed session, you picked up your phone $it times.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                        OutlinedTextField(
                            value = reflectionText,
                            onValueChange = { reflectionText = it },
                            label = { Text("Your reflection...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                            ),
                            maxLines = 5,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                capitalization = KeyboardCapitalization.Sentences,
                            ),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("How much energy do you have now?", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            EnergyLevel.entries.forEach { level ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedEnergyLevel = level }
                                        .background(
                                            if (selectedEnergyLevel == level) {
                                                level.toColor().copy(alpha = 0.2f)
                                            } else {
                                                Color.Transparent
                                            }
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (selectedEnergyLevel == level) {
                                                level.toColor()
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(level.name, color = level.toColor())
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        onSave(reflectionText, selectedEnergyLevel)
                    }) {
                        Text("Save & Close")
                    }
                }
            )
        }
    }
}
