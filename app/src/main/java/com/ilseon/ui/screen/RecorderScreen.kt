package com.ilseon.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilseon.RecorderState
import com.ilseon.ui.theme.MutedTeal
import kotlinx.coroutines.launch

@Composable
fun RecorderScreen(
    recorderState: RecorderState,
    durationSeconds: Int,
    onStartRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    if (recorderState == RecorderState.Saving) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Timer
            Text(
                text = formatDuration(durationSeconds),
                fontSize = 64.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(48.dp))

            // Waveform
            if (recorderState == RecorderState.Recording) {
                PulsingWaveform()
            } else {
                Box(modifier = Modifier.height(50.dp)) // Maintain space
            }
            Spacer(modifier = Modifier.height(48.dp))

            // Buttons based on state
            when (recorderState) {
                RecorderState.Idle -> {
                    // Large Record Button
                    IconButton(
                        onClick = onStartRecording,
                        modifier = Modifier.size(80.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MutedTeal,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Start Recording",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                RecorderState.Recording -> {
                    // Pause and Stop Buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = onPauseRecording,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause Recording",
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(24.dp))
                        IconButton(
                            onClick = onStopRecording,
                            modifier = Modifier.size(80.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop Recording",
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
                RecorderState.Paused -> {
                    // Resume and Stop Buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = onResumeRecording,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume Recording",
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(24.dp))
                        IconButton(
                            onClick = onStopRecording,
                            modifier = Modifier.size(80.dp),
                             colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop Recording",
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
                RecorderState.Stopped -> {
                    // Save and Discard Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onCancel,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = "Cancel")
                            Spacer(Modifier.width(8.dp))
                            Text("Discard")
                        }
                        Spacer(Modifier.width(24.dp))
                        Button(
                            onClick = onSave,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Save")
                            Spacer(Modifier.width(8.dp))
                            Text("Save")
                        }
                    }
                }
                RecorderState.Saving -> {
                    // Show a loading indicator or "Saving..." text
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Saving...",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
//                RecorderState.Starting -> {
//                    // Show a loading indicator
//                    CircularProgressIndicator(
//                        modifier = Modifier.size(48.dp),
//                        color = MaterialTheme.colorScheme.primary
//                    )
//                    Text(
//                        text = "Starting...",
//                        style = MaterialTheme.typography.titleMedium,
//                        modifier = Modifier.padding(top = 16.dp)
//                    )
//                }
            }
        }
    }
}

@Composable
private fun PulsingWaveform() {
    val animatables = List(5) {
        remember { Animatable(1f) }
    }

    LaunchedEffect(Unit) {
        animatables.forEachIndexed { index, animatable ->
            launch {
                animatable.animateTo(
                    targetValue = 5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = (500..1000).random(),
                            delayMillis = (100..500).random(),
                            easing = LinearEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.6f)
            .height(30.dp)
    ) {
        val lineCount = 5
        val lineWidth = size.width / (lineCount * 2)
        val maxLineHeight = size.height
        val center = size.width / 2

        for (i in 0 until lineCount) {
            val animValue = animatables[i].value
            val lineHeight = (maxLineHeight / 5) * animValue
            val x = center + (i - lineCount / 2) * lineWidth * 2

            drawLine(
                color = MutedTeal,
                start = Offset(x, (size.height - lineHeight) / 2),
                end = Offset(x, (size.height + lineHeight) / 2),
                strokeWidth = lineWidth * 0.8f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}