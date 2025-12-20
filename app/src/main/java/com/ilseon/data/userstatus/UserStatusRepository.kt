package com.ilseon.data.userstatus

import com.ilseon.data.EnergyLevel
import com.ilseon.data.userstatus.UserStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserStatusRepository @Inject constructor(
    private val userStatusDao: UserStatusDao
) {
    fun getStatus(userId: String): Flow<UserStatus?> = userStatusDao.getStatus(userId)

    suspend fun saveStatus(userStatus: UserStatus) {
        userStatusDao.upsert(userStatus)
    }

    suspend fun updateUserEnergyLevel(energyLevel: EnergyLevel) {
        val userId = "user"
        val existingStatus = userStatusDao.getStatusOnce(userId)
        val newStatus = existingStatus?.copy(
            currentEnergy = energyLevel,
            lastUpdated = System.currentTimeMillis()
        ) ?: UserStatus(
            userId = userId,
            currentEnergy = energyLevel,
            lastUpdated = System.currentTimeMillis()
        )
        userStatusDao.upsert(newStatus)
    }

}
