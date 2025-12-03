package com.ilseon.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    // Accent Colors
    primary = MutedRed,    // Used for primary actions, FAB, etc.
    secondary = MutedTeal, // Used for secondary accents/highlights.

    // Backgrounds (The surface the elements sit on)
    background = DarkGrey, // Main screen background
    surface = LightGrey,   // Card and component background

    // "ON" Colors (The text/icon color used ON the surface/background)

    // 1. Primary Text/Icons: Used for main headings and body copy.
    onPrimary = CustomTextPrimary,
    onSecondary = CustomTextPrimary,
    onBackground = CustomTextPrimary,
    onSurface = CustomTextPrimary, // Main text on cards will be #E0E0E0

    // 2. Secondary/Muted Text/Icons: CRITICAL FIX. Used for captions, hints, and details.
    onSurfaceVariant = CustomTextSecondary, // Secondary text will be #B0B0B0 (softer grey)

    // Destructive Actions (Always use a dark, low-saturation red for this)
    error = Color(0xFFCF6679), // A standard Material Dark Theme Error color
    onError = Color.Black
)

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