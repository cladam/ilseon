package com.ilseon.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun StreakIndicator(streak: Int, modifier: Modifier = Modifier) {
    val mutedGold = Color(0xFFC9B464)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when {
            streak >= 7 -> { // Mastery Badge
                Icon(
                    imageVector = Icons.Filled.WorkspacePremium,
                    contentDescription = "Mastery Badge: $streak",
                    tint = mutedGold,
                    modifier = Modifier.fillMaxSize()
                )
            }
            streak >= 5 -> { // Deep Focus - Subtle Alpha Pulse (Breathing Effect)
                val infiniteTransition = rememberInfiniteTransition(label = "streak-pulse")

                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.7f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "streak-pulse-alpha"
                )

                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Deep Focus Streak: $streak",
                    tint = mutedGold,
                    modifier = Modifier.fillMaxSize(0.9f).alpha(pulseAlpha)
                )
            }
            streak >= 3 -> { // Momentum
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Momentum Streak: $streak",
                    tint = mutedGold,
                    modifier = Modifier.fillMaxSize(0.9f)
                )
            }
            streak >= 1 -> { // Initiation
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(mutedGold, CircleShape)
                )
            }
        }
    }
}
