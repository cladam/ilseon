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
    // Create a copy of the background color with 80% opacity (20% transparency)
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
        tertiary = TextSecondary,
        onTertiary = DarkGrey,
        error = QuietAmber,
        onError = Color.Black,
    )

    val colors = ColorProviders(
        dark = widgetColorScheme,
        light = widgetColorScheme // Using the same for light theme as it's dark-only
    )
}