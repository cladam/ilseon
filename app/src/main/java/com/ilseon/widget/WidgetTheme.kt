package com.ilseon.widget

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.glance.material3.ColorProviders
import com.ilseon.ui.theme.DarkGrey
import com.ilseon.ui.theme.LightGrey
import com.ilseon.ui.theme.QuietAmber
import com.ilseon.ui.theme.TextPrimary
import com.ilseon.ui.theme.TextSecondary

object WidgetTheme {
    // 1. Define the "Ilseon Palette" locally for the widget
    private val MutedAmber = Color(0xFFC08A3E)  // Idea
    private val MutedTeal = Color(0xFF5A9B80)   // Voice
    private val MutedRed = Color(0xFFB35F5F)    // Task

    private val DarkGrey = Color(0xFF121212)
    private val LightGrey = Color(0xFF1E1E1E)
    private val TextPrimary = Color(0xFFE0E0E0)
    private val TextSecondary = Color(0xFF9E9E9E)

    // 2. The 80% opacity background you requested
    private val transparentBackground = DarkGrey.copy(alpha = 0.8f)

    private val widgetColorScheme = darkColorScheme(
        background = transparentBackground,
        onBackground = TextPrimary,
        surface = LightGrey,
        onSurface = TextPrimary,
        primary = TextPrimary,
        onPrimary = DarkGrey,
        secondary = TextSecondary,
        onSecondary = DarkGrey,
        // We can map our Semantic colors to unused slots or
        // access them via a custom object (recommended below)
        tertiary = MutedTeal,
        error = MutedAmber
    )

    val colors = ColorProviders(
        dark = widgetColorScheme,
        light = widgetColorScheme
    )

}