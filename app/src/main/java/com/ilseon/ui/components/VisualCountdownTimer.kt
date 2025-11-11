package com.ilseon.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ilseon.ui.theme.IlseonTheme
import com.ilseon.ui.theme.MutedTeal
import com.ilseon.ui.theme.QuietAmber
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

private const val FIVE_MINUTES_IN_MILLIS = 5 * 60 * 1000L

@Composable
fun VisualCountdownTimer(
    modifier: Modifier = Modifier,
    totalTimeInMillis: Long,
    remainingTimeInMillis: Long,
    size: Dp = 200.dp,
    strokeWidth: Dp = 12.dp
) {
    val progress = remainingTimeInMillis.toFloat() / totalTimeInMillis.toFloat()
    val isTimeLow = remainingTimeInMillis <= FIVE_MINUTES_IN_MILLIS

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val pulsingColor by infiniteTransition.animateColor(
        initialValue = QuietAmber,
        targetValue = QuietAmber.copy(alpha = 0.7f),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse_color_low_time"
    )

    val subtlePulseColor by infiniteTransition.animateColor(
        initialValue = MutedTeal,
        targetValue = MutedTeal.copy(alpha = 0.8f),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "subtle_pulse_color"
    )

    val arcColor = if (isTimeLow) pulsingColor else subtlePulseColor

    fun formatTime(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) -
                TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%02d:%02d", minutes, seconds)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            // Background arc
            drawArc(
                color = Color.Gray.copy(alpha = 0.3f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
                size = Size(size.toPx(), size.toPx()),
                topLeft = Offset.Zero
            )

            // Foreground arc
            drawArc(
                color = arcColor,
                startAngle = -90f,
                sweepAngle = 360 * progress,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
                size = Size(size.toPx(), size.toPx()),
                topLeft = Offset.Zero
            )
        }
        Text(
            text = formatTime(remainingTimeInMillis),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun VisualCountdownTimerPreview() {
    var remainingTime by remember { mutableStateOf(30 * 60 * 1000L) }
    val totalTime = 30 * 60 * 1000L

    LaunchedEffect(Unit) {
        while (remainingTime > 0) {
            delay(1000)
            remainingTime -= 1000
        }
    }

    IlseonTheme {
        VisualCountdownTimer(
            totalTimeInMillis = totalTime,
            remainingTimeInMillis = remainingTime
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun VisualCountdownTimerLowTimePreview() {
    var remainingTime by remember { mutableStateOf(4 * 60 * 1000L) }
    val totalTime = 30 * 60 * 1000L

    LaunchedEffect(Unit) {
        while (remainingTime > 0) {
            delay(1000)
            remainingTime -= 1000
        }
    }

    IlseonTheme {
        VisualCountdownTimer(
            totalTimeInMillis = totalTime,
            remainingTimeInMillis = remainingTime
        )
    }
}