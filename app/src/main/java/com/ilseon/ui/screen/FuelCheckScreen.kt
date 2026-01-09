package com.ilseon.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery1Bar
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ilseon.FuelCheckViewModel
import com.ilseon.data.EnergyLevel
import com.ilseon.ui.theme.EnergyHigh
import com.ilseon.ui.theme.EnergyLow
import com.ilseon.ui.theme.EnergyMedium
import com.ilseon.ui.theme.IlseonTheme

@Composable
fun FuelCheckScreen(
    viewModel: FuelCheckViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val userStatus by viewModel.userStatus.collectAsState()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Fuel Check", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "How much energy do you have now?",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            EnergyLevel.entries.forEach { energyLevel ->
                EnergyButton(
                    energyLevel = energyLevel,
                    isSelected = userStatus?.currentEnergy == energyLevel,
                    onClick = { 
                        viewModel.updateUserEnergyLevel(energyLevel) {
                            onNavigateBack()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun EnergyButton(
    energyLevel: EnergyLevel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = energyLevel.toColor().copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (energyLevel) {
                        EnergyLevel.High -> Icons.Default.BatteryFull
                        EnergyLevel.Medium -> Icons.Default.Battery3Bar
                        EnergyLevel.Low -> Icons.Default.Battery1Bar
                    },
                    contentDescription = null,
                    tint = energyLevel.toColor()
                )
                Text(
                    text = energyLevel.name,
                    modifier = Modifier.padding(start = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color = energyLevel.toColor(), shape = CircleShape)
                )
            }
        }
    }
}

fun EnergyLevel.toColor(): Color {
    return when (this) {
        EnergyLevel.High -> EnergyHigh
        EnergyLevel.Medium -> EnergyMedium
        EnergyLevel.Low -> EnergyLow
    }
}
