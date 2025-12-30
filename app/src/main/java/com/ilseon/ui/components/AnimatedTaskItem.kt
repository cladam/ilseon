package com.ilseon.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.ilseon.data.task.Task
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AnimatedTaskItem(
    task: Task,
    isVisible: Boolean,
    onComplete: (Task) -> Unit,
    onHaptic: () -> Unit,
    reduceMotion: Boolean = false,
    content: @Composable (Task) -> Unit
) {
    val scope = rememberCoroutineScope()

    // Using Animatable for more control over velocity and "snapping"
    val animScale = remember { Animatable(1f) }
    val animAlpha = remember { Animatable(1f) }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            // 1. Trigger Haptic immediately on 'intent to complete'
            onHaptic()

            // 2. Parallel organic exit
            scope.launch {
                animAlpha.animateTo(0f, tween(if (reduceMotion) 200 else 400))
            }

            if (!reduceMotion) {
                scope.launch {
                    // "The Sprout Snap": Scale up slightly before disappearing
                    animScale.animateTo(1.03f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                    animScale.animateTo(0.8f, spring(stiffness = Spring.StiffnessLow))
                }
            }

            // Delay until the visual "pop" is done before calling logic
            delay(if (reduceMotion) 200 else 450)
            onComplete(task)
        } else {
            // Reset state if it becomes visible again (e.g., undo)
            animScale.snapTo(1f)
            animAlpha.snapTo(1f)
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = if (reduceMotion) {
            fadeOut(tween(200))
        } else {
            // Directional exit: slide slightly up to imply it's "uploaded" to the pulse
            slideOutVertically(targetOffsetY = { -it / 4 }) +
                    shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) +
                    fadeOut()
        }
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    // Optimization: Use graphicsLayer variables instead of separate modifiers
                    scaleX = animScale.value
                    scaleY = animScale.value
                    alpha = animAlpha.value
                    // Slight rotation makes it feel less "perfectly digital"
                    rotationZ = if (!reduceMotion) (1f - animScale.value) * 10f else 0f
                }
        ) {
            content(task)
        }
    }
}
