package com.ilseon.data.voicememo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
}
