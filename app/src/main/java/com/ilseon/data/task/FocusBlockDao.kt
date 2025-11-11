package com.ilseon.data.task

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusBlockDao {

    @Query("SELECT * FROM focus_blocks")
    fun getFocusBlocks(): Flow<List<FocusBlock>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(focusBlock: FocusBlock)
}
