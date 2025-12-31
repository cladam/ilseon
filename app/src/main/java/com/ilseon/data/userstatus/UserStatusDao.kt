package com.ilseon.data.userstatus

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ilseon.data.EnergyLevel
import com.ilseon.data.userstatus.UserStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStatusDao {
    @Upsert
    suspend fun upsert(userStatus: UserStatus)

    @Query("SELECT * FROM userstatus WHERE userId = :userId")
    fun getStatus(userId: String): Flow<UserStatus?>

    @Query("SELECT * FROM userstatus LIMIT 1")
    suspend fun getUserStatus(): UserStatus?


    @Query("UPDATE userstatus SET currentEnergy = :energyLevel, lastUpdated = :timestamp WHERE userId = :userId")
    suspend fun updateEnergyLevel(userId: String, energyLevel: EnergyLevel, timestamp: Long)

    @Query("SELECT * FROM userstatus WHERE userId = :userId")
    suspend fun getStatusOnce(userId: String): UserStatus?

}
