package com.ilseon.data.task

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusBlockRepository @Inject constructor(
    private val focusBlockDao: FocusBlockDao
) {
    fun getFocusBlocks(): Flow<List<FocusBlock>> = focusBlockDao.getFocusBlocks()
}
