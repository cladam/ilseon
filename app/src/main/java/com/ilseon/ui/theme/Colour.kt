package com.ilseon.ui.theme

import androidx.compose.ui.graphics.Color
import com.ilseon.data.task.TaskPriority

// Dark, low-saturation palette
val AccentRed = Color(0xFF8B0000) // Accent for FAB, as per mockup
val Teal = Color(0xFF03DAC5) // A subtle secondary accent

val DarkGrey = Color(0xFF121212)      // Primary background
val LightGrey = Color(0xFF1E1E1E)     // Surface color (cards)
val TextPrimary = Color(0xFFE0E0E0)   // Primary text color
val TextSecondary = Color(0xFFB0B0B0) // Secondary text color

// "Low Sensory" Accents (From our comparison)
val MutedRed = Color(0xFFB35F5F)      // For FAB (Primary Action)
val MutedTeal = Color(0xFF4C9A9B)     // For Task Borders (Secondary Highlight)
val QuietAmber = Color(0xFFC08A3E)    // For timer alerts

val PriorityHigh = MutedRed
val PriorityMedium = QuietAmber
val PriorityLow = TextSecondary

fun TaskPriority.toColor(): Color {
    return when(this) {
        TaskPriority.High -> PriorityHigh
        TaskPriority.Mid -> PriorityMedium
        TaskPriority.Low -> PriorityLow
    }
}
