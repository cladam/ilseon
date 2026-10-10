package com.ilseon.data.routine

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface RoutineDao {

    // --- Routine CRUD ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: Routine): Long

    @Update
    suspend fun updateRoutine(routine: Routine)

    @Delete
    suspend fun deleteRoutine(routine: Routine)

    @Query("SELECT * FROM routines WHERE id = :routineId")
    suspend fun getRoutineById(routineId: UUID): Routine?

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :routineId")
    suspend fun getRoutineWithStepsById(routineId: UUID): RoutineWithSteps?

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :routineId")
    fun observeRoutineWithStepsById(routineId: UUID): Flow<RoutineWithSteps?>

    @Transaction
    @Query("SELECT * FROM routines WHERE isArchived = 0 ORDER BY displayOrder ASC, createdAt ASC")
    fun observeActiveRoutinesWithSteps(): Flow<List<RoutineWithSteps>>

    @Transaction
    @Query("SELECT * FROM routines WHERE contextId = :contextId AND isArchived = 0 ORDER BY displayOrder ASC, createdAt ASC")
    fun observeActiveRoutinesByContext(contextId: UUID): Flow<List<RoutineWithSteps>>

    @Transaction
    @Query("SELECT * FROM routines WHERE isArchived = 1 ORDER BY createdAt DESC")
    fun observeArchivedRoutinesWithSteps(): Flow<List<RoutineWithSteps>>

    // --- Routine Steps CRUD ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: RoutineStep): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<RoutineStep>): List<Long>

    @Update
    suspend fun updateStep(step: RoutineStep)

    @Update
    suspend fun updateSteps(steps: List<RoutineStep>)

    @Delete
    suspend fun deleteStep(step: RoutineStep)

    @Query("DELETE FROM routine_steps WHERE id = :stepId")
    suspend fun deleteStepById(stepId: UUID)

    @Query("DELETE FROM routine_steps WHERE routineId = :routineId")
    suspend fun deleteStepsForRoutine(routineId: UUID)

    @Query("SELECT * FROM routine_steps WHERE routineId = :routineId ORDER BY orderIndex ASC")
    suspend fun getStepsForRoutine(routineId: UUID): List<RoutineStep>

    // --- Routine Runs (Execution Sessions) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: RoutineRun): Long

    @Update
    suspend fun updateRun(run: RoutineRun)

    @Query("SELECT * FROM routine_runs WHERE id = :runId")
    suspend fun getRunById(runId: UUID): RoutineRun?

    @Query("SELECT * FROM routine_runs WHERE routineId = :routineId AND sessionDate = :sessionDate LIMIT 1")
    suspend fun getRunForRoutineAndSessionDate(routineId: UUID, sessionDate: Long): RoutineRun?

    @Query("SELECT * FROM routine_runs WHERE routineId = :routineId AND sessionDate = :sessionDate LIMIT 1")
    fun observeRunForRoutineAndSessionDate(routineId: UUID, sessionDate: Long): Flow<RoutineRun?>

    @Query("SELECT * FROM routine_runs WHERE routineId = :routineId ORDER BY startedAt DESC LIMIT :limit")
    suspend fun getRecentRunsForRoutine(routineId: UUID, limit: Int = 10): List<RoutineRun>
}
