package com.ilseon.data.userstatus

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ilseon.data.EnergyLevel

@Entity
data class UserStatus(
    @PrimaryKey val userId: String,
    // The most recent Fuel Check result
    val currentEnergy: EnergyLevel = EnergyLevel.Medium,
    val lastUpdated: Long = System.currentTimeMillis()
)