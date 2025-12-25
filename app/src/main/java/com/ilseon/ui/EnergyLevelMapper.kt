package com.ilseon.ui

import com.ilseon.data.EnergyLevel

fun mapEffortToEnum(effort: String): EnergyLevel {
    return when (effort.lowercase()) {
        "high" -> EnergyLevel.High
        "medium" -> EnergyLevel.Medium
        "low" -> EnergyLevel.Low
        else -> EnergyLevel.Medium // Default value
    }
}
