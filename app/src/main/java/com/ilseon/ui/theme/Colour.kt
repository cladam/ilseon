package com.ilseon.ui.theme

import androidx.compose.ui.graphics.Color
import com.ilseon.data.EnergyLevel
import com.ilseon.data.task.TaskPriority

val TealAccent = Color(0xFF00BFA5)

// Dark, low-saturation palette
val AccentRed = Color(0xFF8B0000) // Accent for FAB, as per mockup
val Teal = Color(0xFF03DAC5) // A subtle secondary accent

// New
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

val TextPrimary = Color(0xFFE0E0E0)   // Primary text color
val TextSecondary = Color(0xFFB0B0B0) // Secondary text color
// --- New colours
//
//
//val DarkGrey = Color(0xFF121212)      // Primary background
//val LightGrey = Color(0xFF1E1E1E)     // Surface color (cards)
//val TextPrimary = Color(0xFFE0E0E0)   // Primary text color
//val TextSecondary = Color(0xFFB0B0B0) // Secondary text color
//// Use these custom colors for semantic slots
//val CustomTextPrimary = Color(0xFFB0B0B0)   // Primary text color (Your preferred soft white)
//val CustomTextSecondary = Color(0xFF888888) // Secondary text color (Your muted detail color)
//
//
//// "Low Sensory" Accents (From our comparison)
//val MutedRed = Color(0xFFB35F5F)      // For FAB (Primary Action)
//val MutedTeal = Color(0xFF4C9A9B)     // For Task Borders (Secondary Highlight)
//val QuietAmber = Color(0xFFC08A3E)    // For timer alerts

// Use your original "Low Sensory" Accents for Priority (The "Demands")
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
 */
fun TaskPriority.toColor(): Color = when(this) {
    TaskPriority.High -> PriorityHigh
    TaskPriority.Medium -> PriorityMedium
    TaskPriority.Low -> PriorityLow
}

/*val PriorityHigh = MutedRed
val PriorityMedium = QuietAmber
val PriorityLow = TextSecondary

fun TaskPriority.toColor(): Color {
    return when(this) {
        TaskPriority.High -> PriorityHigh
        TaskPriority.Medium -> PriorityMedium
        TaskPriority.Low -> PriorityLow
    }
}

// Energy-based Colors
val MutedGreen = Color(0xFF5A9B6E)
val MutedGold = QuietAmber

val EnergyHigh = MutedGreen
val EnergyMedium = MutedGold
val EnergyLow = MutedRed*/

// --- New colours
//
//// --- The Base Foundation (OLED Focused) ---
//val BackgroundDeep = Color(0xFF050505) // True black for maximum focus
//val SurfaceDark = Color(0xFF0D0D0D)    // Slightly lifted for cards
//val SurfaceMedium = Color(0xFF161616)  // For secondary elements
//val BorderQuiet = Color(0xFF1F1F1F)    // Subtle borders
//
//// --- The Refined Text Stack ---
//val TextPrimary = Color(0xFFE5E5E5)    // Off-white (less eye strain than pure white)
//val TextSecondary = Color(0xFF888888)  // Muted meta-data
//val TextDisabled = Color(0xFF444444)   // Faded out elements
//
//// --- Semantic Accents (Low Sensory / High Impact) ---
//// Swapping the "Old Teal" for "Sage/Stone"
//val PrimaryAction = Color(0xFFD4D4D4)  // Use Neutral for main actions (Premium feel)
//val AccentFocus = Color(0xFFFFFFFF)    // Pure white only for the "Active Task"
//
//// --- The "Pulse" Spectrum (Energy & Priority) ---
//
//// HIGH: High Energy / High Priority (Organic Gold/Amber)
//val StatusHigh = Color(0xFFE2B05E) // Warm Ochre (Motivating, not alarming)
//
//// MEDIUM: Balanced State (Sage)
//val StatusMedium = Color(0xFFA3A991) // Muted Sage (Calm, steady)
//
//// LOW: Low Energy / Low Priority (Slate/Clay)
//val StatusLow = Color(0xFF7D8597) // Steel Blue/Grey (Low pressure)
//
//// URGENT: The only "Warm" color (Deep Terracotta)
//val StatusUrgent = Color(0xFFB35F5F) // Your Muted Red (Used sparingly)
//
///**
// * Mapping your logic to the new palette
// */
//fun TaskPriority.toColor(): Color {
//    return when(this) {
//        TaskPriority.High -> StatusHigh
//        TaskPriority.Medium -> StatusMedium
//        TaskPriority.Low -> StatusLow
//    }
//}
//
//// Revised Energy Mapping
//// High Energy = You have "Fuel" (Gold)
//// Low Energy = You are "Cold" (Slate)
//fun EnergyLevel.toColor(): Color {
//    return when(this) {
//        EnergyLevel.High -> StatusHigh
//        EnergyLevel.Medium -> StatusMedium
//        EnergyLevel.Low -> StatusLow
//    }
//}
