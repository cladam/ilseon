package com.ilseon.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Semantic mapping for Task Statuses.
 * Using the "Warm for Priority / Cool for Energy" logic to avoid
 * visual confusion in the UI.
 */
object TaskColors {
    // Priority (Warm/Attention Scale)
    val PriorityHigh = MutedRed
    val PriorityMedium = QuietAmber
    val PriorityLow = CustomTextSecondary // Using your hint/caption color

    // Energy (Cool/Capacity Scale)
    val EnergyHigh = MutedTeal
    val EnergyMedium = Color(0xFF5A9B6E) // Muted Sage Green
    val EnergyLow = Color(0xFF5E6D7E)    // Slate/Steel Blue
}

private val DarkColorScheme = darkColorScheme(
    // Accent Colors
//    primary = MutedRed,
//    secondary = MutedTeal,
    primary = MutedTeal,
    secondary = MutedRed,

    // Backgrounds
    background = DarkGrey,
    surface = LightGrey,

    // "ON" Colors
    onPrimary = CustomTextPrimary,
    onSecondary = CustomTextPrimary,
    onBackground = CustomTextPrimary,
    onSurface = CustomTextPrimary,

    // Muted/Caption Text Fix
    onSurfaceVariant = CustomTextSecondary,

    // Error States
    error = Color(0xFFCF6679),
    onError = Color.Black
)

/**
 * Usage Example in a Composable:
 * val priorityColor = when(task.priority) {
 * High -> MaterialTheme.colorScheme.primary // Uses MutedRed
 * Low -> MaterialTheme.colorScheme.onSurfaceVariant // Uses CustomTextSecondary
 * else -> QuietAmber
 * }
 */

@Composable
fun IlseonTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}