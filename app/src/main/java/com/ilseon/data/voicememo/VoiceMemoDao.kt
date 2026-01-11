package com.ilseon.data.voicememo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ilseon.data.ContextDistribution
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceMemoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(voiceMemo: VoiceMemo)

    @Update
    suspend fun update(voiceMemo: VoiceMemo)

    @Query("DELETE FROM voice_memos WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM voice_memos ORDER BY timestamp DESC")
    fun getVoiceMemos(): Flow<List<VoiceMemo>>

    @Query("SELECT * FROM voice_memos WHERE id = :id")
    suspend fun getVoiceMemo(id: String): VoiceMemo?

    @Query("SELECT COUNT(*) FROM voice_memos WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getVoiceMemosCount(startTime: Long, endTime: Long): Int

    @Query("SELECT contextId, COUNT(*) as count FROM voice_memos WHERE timestamp BETWEEN :startTime AND :endTime AND contextId IS NOT NULL GROUP BY contextId")
    suspend fun getContextDistribution(startTime: Long, endTime: Long): List<ContextDistribution>
}
