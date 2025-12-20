package com.ilseon.data

import androidx.compose.ui.graphics.Color
import com.ilseon.ui.theme.EnergyHigh
import com.ilseon.ui.theme.EnergyLow
import com.ilseon.ui.theme.EnergyMedium

enum class EnergyLevel {
    High,
    Medium,
    Low,
}

fun EnergyLevel.toColor(): Color {
    return when (this) {
        EnergyLevel.High -> EnergyHigh
        EnergyLevel.Medium -> EnergyMedium
        EnergyLevel.Low -> EnergyLow
    }
}
