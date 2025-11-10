package com.ilseon.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.ilseon.data.task.Task
import kotlinx.coroutines.delay

@Composable
fun AnimatedTaskItem(
    task: Task,
    isVisible: Boolean,
    onComplete: (Task) -> Unit,
    content: @Composable (Task) -> Unit
) {
    // This effect will run when isVisible changes from true to false
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            delay(300) // Wait for animation to finish
            onComplete(task)
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        exit = shrinkVertically(animationSpec = tween(durationMillis = 300)) +
                fadeOut(animationSpec = tween(durationMillis = 300)),
    ) {
        content(task)
    }
}