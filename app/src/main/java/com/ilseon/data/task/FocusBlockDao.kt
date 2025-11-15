package com.ilseon.data.task

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface FocusBlockDao {

    @Query("SELECT * FROM focus_blocks")
    fun getFocusBlocks(): Flow<List<FocusBlock>>

    @Query("SELECT * FROM focus_blocks WHERE contextId = :contextId")
    suspend fun getFocusBlockForContext(contextId: UUID): FocusBlock?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(focusBlock: FocusBlock)

    @Update
    suspend fun update(focusBlock: FocusBlock)

    @Delete
    suspend fun delete(focusBlock: FocusBlock)
}
