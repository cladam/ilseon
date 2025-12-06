package com.ilseon.data.voicememo

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceMemoRepository @Inject constructor(
    private val voiceMemoDao: VoiceMemoDao
) {
    fun getVoiceMemos() = voiceMemoDao.getVoiceMemos()

    suspend fun insert(voiceMemo: VoiceMemo) {
        voiceMemoDao.insert(voiceMemo)
    }

    suspend fun delete(id: String) {
        voiceMemoDao.delete(id)
    }
}
