package com.ilseon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilseon.data.EnergyLevel
import com.ilseon.data.userstatus.UserStatus
import com.ilseon.data.userstatus.UserStatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FuelCheckViewModel @Inject constructor(
    private val userStatusRepository: UserStatusRepository
) : ViewModel() {

    // Assuming a single user
    val userStatus: StateFlow<UserStatus?> = userStatusRepository.getStatus("user")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateUserEnergyLevel(energyLevel: EnergyLevel, onUpdated: () -> Unit) {
        viewModelScope.launch {
            userStatusRepository.updateUserEnergyLevel(energyLevel)
            onUpdated()
        }
    }
}
