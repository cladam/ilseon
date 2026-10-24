package com.ilseon.ui.theme

import androidx.compose.ui.graphics.Color
import com.ilseon.data.EnergyLevel
import com.ilseon.data.task.TaskPriority

val TealAccent = Color(0xFF00BFA5)

// Dark, low-saturation palette
val AccentRed = Color(0xFF8B0000) // Accent for FAB, as per mockup
val Teal = Color(0xFF03DAC5) // A subtle secondary accent

// --- Color Palette Definitions ---
val MutedRed = Color(0xFFB35F5F)
// Shifted from 4C9A9B (Blue-Teal) to 5A9B80 (Green-Teal)
val MutedTeal = Color(0xFF5A9B80)
val BlueTeal = Color(0xFF4C9A9B)
val MutedGreen = Color(0xFF5A9B6E)
val BorderQuiet = Color(0xFF1F1F1F) // BorderQuiet
val MutedDetail = Color(0xFF888888)
val QuietAmber = Color(0xFFC08A3E)
val DarkGrey = Color(0xFF121212)
val LightGrey = Color(0xFF1E1E1E)
val SlateBlue = Color(0xFF5E6D7E)
val CustomTextPrimary = Color(0xFFB0B0B0)   // Primary text color (Your preferred soft white)
val CustomTextSecondary = Color(0xFF888888) // Secondary text color (Your muted detail color)


// ==========================================
// Light Mode Foundations (Low-Sensory / Soft)
// ==========================================
// Soft warm/neutral canvas (prevents white-screen glare)
val LightBackground = Color(0xFFF3F5F4)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE8ECE9) // For secondary rows like "Next Up"
val LightBorderQuiet = Color(0xFFD5DDD9)    // Delicate card and divider borders

// ==========================================
// Light Mode Typography (Charcoal over harsh black)
// ==========================================
val LightTextPrimary = Color(0xFF1B2423)     // Soft deep charcoal
val LightTextSecondary = Color(0xFF5B6764)   // Legible muted slate
val LightTextDisabled = Color(0xFF9EABA7)

// ==========================================
// Light Mode Semantic Accents
// ==========================================
// Deepened version of your MutedTeal for solid buttons and readable icons
val LightPrimaryTeal = Color(0xFF2E7B6C)
// Mockup ring / border accent (matches MutedTeal / BlueTeal)
val LightAccentTeal = Color(0xFF4C9A9B)

val LightPriorityHigh = Color(0xFFA84848)   // Slightly deeper MutedRed for light mode
val LightPriorityMedium = Color(0xFF9E6E27) // Slightly deeper QuietAmber
val LightPriorityLow = LightTextSecondary

val TextPrimary = Color(0xFFE0E0E0)   // Primary text color
val TextSecondary = Color(0xFFB0B0B0) // Secondary text color

// Original "Low Sensory" Accents for Priority (The "Demands")
val PriorityHigh = MutedRed  // Muted Red (Urgent)
val PriorityMedium =QuietAmber // Quiet Amber (Important)
val PriorityLow = MutedDetail // Muted Detail (Backburner)

// --- The "Pulse" Spectrum (Energy & Priority) ---
// HIGH: High Energy / High Priority (Organic Gold/Amber)
val StatusHigh = Color(0xFFE2B05E) // Warm Ochre (Motivating, not alarming)
// MEDIUM: Balanced State (Sage)
val StatusMedium = Color(0xFFA3A991) // Muted Sage (Calm, steady)
// LOW: Low Energy / Low Priority (Slate/Clay)
val StatusLow = Color(0xFF7D8597) // Steel Blue/Grey (Low pressure)
// URGENT: The only "Warm" color (Deep Terracotta)
val StatusUrgent = Color(0xFFB35F5F) // Muted Red (Used sparingly)

val EnergyHigh = StatusHigh //BlueTeal
val EnergyMedium = StatusMedium //MutedGreen
val EnergyLow = StatusLow //SlateBlue

/**
 * Priority: How much does the world want from me? (Warm Scale)
 * Non-composable fallback (defaults to dark-theme palette, e.g. for widgets or background notifications).
 */
fun TaskPriority.toColor(): Color = when(this) {
    TaskPriority.High -> PriorityHigh
    TaskPriority.Medium -> PriorityMedium
    TaskPriority.Low -> PriorityLow
}

