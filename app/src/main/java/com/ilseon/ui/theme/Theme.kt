package com.ilseon.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.ilseon.data.EnergyLevel
import com.ilseon.data.task.TaskPriority

enum class AppThemeMode {
    Dark,
    Light,
    Auto
}

/**
 * Custom semantic colors for domain concepts like Priority, Energy, and subtle borders.
 */
@Immutable
data class ExtendedColors(
    val priorityHigh: Color,
    val priorityMedium: Color,
    val priorityLow: Color,
    val energyHigh: Color,
    val energyMedium: Color,
    val energyLow: Color,
    val borderQuiet: Color,
    val detailText: Color,
    val accentTeal: Color
)

val DarkExtendedColors = ExtendedColors(
    priorityHigh = PriorityHigh,
    priorityMedium = PriorityMedium,
    priorityLow = PriorityLow,
    energyHigh = EnergyHigh,
    energyMedium = EnergyMedium,
    energyLow = EnergyLow,
    borderQuiet = BorderQuiet,
    detailText = MutedDetail,
    accentTeal = MutedTeal
)

val LightExtendedColors = ExtendedColors(
    priorityHigh = LightPriorityHigh,
    priorityMedium = LightPriorityMedium,
    priorityLow = LightPriorityLow,
    energyHigh = StatusHigh,
    energyMedium = StatusMedium,
    energyLow = StatusLow,
    borderQuiet = LightBorderQuiet,
    detailText = LightTextSecondary,
    accentTeal = LightAccentTeal
)

val LocalExtendedColors = staticCompositionLocalOf { DarkExtendedColors }

val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current

@Composable
fun TaskPriority.toThemeColor(): Color {
    val colors = MaterialTheme.extendedColors
    return when (this) {
        TaskPriority.High -> colors.priorityHigh
        TaskPriority.Medium -> colors.priorityMedium
        TaskPriority.Low -> colors.priorityLow
    }
}

@Composable
fun EnergyLevel.toThemeColor(): Color {
    val colors = MaterialTheme.extendedColors
    return when (this) {
        EnergyLevel.High -> colors.energyHigh
        EnergyLevel.Medium -> colors.energyMedium
        EnergyLevel.Low -> colors.energyLow
    }
}

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
    primary = MutedTeal,
    secondary = MutedRed,

    // Backgrounds
    background = DarkGrey,
    surface = LightGrey,
    surfaceVariant = Color(0xFF242424),
    outline = BorderQuiet,

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

private val LightColorScheme = lightColorScheme(
    primary = LightPrimaryTeal,
    secondary = LightPriorityHigh,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    outline = LightBorderQuiet,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary,
    error = LightPriorityHigh,
    onError = Color.White
)

@Composable
fun IlseonTheme(
    themeMode: AppThemeMode = AppThemeMode.Dark,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.Dark -> true
        AppThemeMode.Light -> false
        AppThemeMode.Auto -> systemInDark
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val extendedColors = if (isDark) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
