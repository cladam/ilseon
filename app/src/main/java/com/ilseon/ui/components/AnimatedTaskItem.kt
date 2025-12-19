package com.ilseon.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import com.ilseon.data.task.Task
import kotlinx.coroutines.delay

@Composable
fun AnimatedTaskItem(
    task: Task,
    isVisible: Boolean,
    onComplete: (Task) -> Unit,
    content: @Composable (Task) -> Unit
) {
    var animationStarted by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            animationStarted = true
            delay(500) // Wait for animation to finish
            onComplete(task)
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (animationStarted) 0.8f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (animationStarted) 0.5f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "alpha"
    )

    AnimatedVisibility(
        visible = isVisible,
        exit = slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = 400)
        ) + shrinkVertically(
            animationSpec = tween(durationMillis = 300, delayMillis = 100)
        ) + fadeOut(
            animationSpec = tween(durationMillis = 300)
        )
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .alpha(alpha)
        ) {
            content(task)
        }
    }
}
