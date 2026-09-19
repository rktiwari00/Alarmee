package app.wakewalk.domain.repository

import app.wakewalk.data.local.entity.AlarmHistoryEntity
import app.wakewalk.domain.model.WakeSession
import kotlinx.coroutines.flow.StateFlow

interface WakeSessionRepository {
    val activeSessionFlow: StateFlow<WakeSession?>
    suspend fun restoreSessionFromDb(): WakeSession?
    suspend fun startSession(session: WakeSession)
    suspend fun updateSession(session: WakeSession)
    suspend fun completeSession(history: AlarmHistoryEntity)
    suspend fun clearSession()
}
