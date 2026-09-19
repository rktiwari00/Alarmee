package app.wakewalk.data.repository

import app.wakewalk.data.local.WakeWalkDatabase
import app.wakewalk.data.local.dao.ActiveSessionDao
import app.wakewalk.data.local.entity.ActiveSessionEntity
import app.wakewalk.data.local.entity.AlarmHistoryEntity
import app.wakewalk.domain.model.WakeSession
import app.wakewalk.domain.repository.WakeSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WakeSessionRepositoryImpl @Inject constructor(
    private val activeSessionDao: ActiveSessionDao,
    private val database: WakeWalkDatabase
) : WakeSessionRepository {

    private val mutex = Mutex()
    private val _activeSessionFlow = MutableStateFlow<WakeSession?>(null)
    override val activeSessionFlow: StateFlow<WakeSession?> = _activeSessionFlow.asStateFlow()

    override suspend fun restoreSessionFromDb(): WakeSession? = mutex.withLock {
        val entity = activeSessionDao.getActiveSession() ?: return null
        val restored = WakeSession(
            sessionId = entity.sessionId,
            alarmId = entity.alarmId,
            targetSteps = entity.targetSteps,
            initialSensorSteps = entity.initialSensorSteps,
            currentSteps = entity.currentSteps,
            status = entity.status,
            startedAtEpochMs = entity.startedAtEpochMs,
            startedRealtimeNs = entity.startedRealtimeNs,
            trackingMode = entity.trackingMode
        )
        _activeSessionFlow.value = restored
        restored
    }

    override suspend fun startSession(session: WakeSession) = mutex.withLock {
        _activeSessionFlow.value = session
        activeSessionDao.upsertSession(
            ActiveSessionEntity(
                singletonId = 1,
                sessionId = session.sessionId,
                alarmId = session.alarmId,
                targetSteps = session.targetSteps,
                initialSensorSteps = session.initialSensorSteps,
                currentSteps = session.currentSteps,
                startedAtEpochMs = session.startedAtEpochMs,
                startedRealtimeNs = session.startedRealtimeNs,
                status = session.status,
                trackingMode = session.trackingMode,
                lastUpdatedEpochMs = System.currentTimeMillis()
            )
        )
    }

    override suspend fun updateSession(session: WakeSession) = mutex.withLock {
        _activeSessionFlow.value = session
        activeSessionDao.upsertSession(
            ActiveSessionEntity(
                singletonId = 1,
                sessionId = session.sessionId,
                alarmId = session.alarmId,
                targetSteps = session.targetSteps,
                initialSensorSteps = session.initialSensorSteps,
                currentSteps = session.currentSteps,
                startedAtEpochMs = session.startedAtEpochMs,
                startedRealtimeNs = session.startedRealtimeNs,
                status = session.status,
                trackingMode = session.trackingMode,
                lastUpdatedEpochMs = System.currentTimeMillis()
            )
        )
    }

    override suspend fun completeSession(history: AlarmHistoryEntity) = mutex.withLock {
        database.completeActiveSession(history)
        _activeSessionFlow.value = null
    }

    override suspend fun clearSession() = mutex.withLock {
        activeSessionDao.clearActiveSession()
        _activeSessionFlow.value = null
    }
}
