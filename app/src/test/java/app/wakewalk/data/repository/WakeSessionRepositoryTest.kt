package app.wakewalk.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.wakewalk.data.local.WakeWalkDatabase
import app.wakewalk.data.local.entity.AlarmHistoryEntity
import app.wakewalk.domain.model.AlarmStatus
import app.wakewalk.domain.model.StepTrackingMode
import app.wakewalk.domain.model.WakeSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class WakeSessionRepositoryTest {

    private lateinit var db: WakeWalkDatabase
    private lateinit var repository: WakeSessionRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WakeWalkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WakeSessionRepositoryImpl(db.activeSessionDao(), db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testStartSessionAndObserveStateFlow() = runBlocking {
        val session = WakeSession(
            sessionId = "session-test-1",
            alarmId = 42L,
            targetSteps = 150,
            initialSensorSteps = 2000L,
            currentSteps = 0,
            status = AlarmStatus.CHALLENGE_ACTIVE,
            startedAtEpochMs = 1000L,
            startedRealtimeNs = 2000L,
            trackingMode = StepTrackingMode.HARDWARE_COUNTER
        )

        repository.startSession(session)
        val current = repository.activeSessionFlow.value
        assertNotNull(current)
        assertEquals("session-test-1", current!!.sessionId)

        // Verify persisted to DB
        val inDb = db.activeSessionDao().getActiveSession()
        assertNotNull(inDb)
        assertEquals("session-test-1", inDb!!.sessionId)
    }

    @Test
    fun testSessionRestorationAfterSimulatedProcessDeath() = runBlocking {
        val session = WakeSession(
            sessionId = "session-test-2",
            alarmId = 42L,
            targetSteps = 150,
            initialSensorSteps = 2000L,
            currentSteps = 73, // 73 steps already walked before process interrupted
            status = AlarmStatus.CHALLENGE_ACTIVE,
            startedAtEpochMs = 1000L,
            startedRealtimeNs = 2000L,
            trackingMode = StepTrackingMode.HARDWARE_COUNTER
        )

        repository.startSession(session)

        // Create a new repository instance to simulate process relaunch
        val newRepoInstance = WakeSessionRepositoryImpl(db.activeSessionDao(), db)
        assertNull(newRepoInstance.activeSessionFlow.value) // In-memory initially null

        val restored = newRepoInstance.restoreSessionFromDb()
        assertNotNull(restored)
        assertEquals("session-test-2", restored!!.sessionId)
        assertEquals(73, restored.currentSteps)
        assertEquals(newRepoInstance.activeSessionFlow.value, restored)
    }

    @Test
    fun testAtomicSessionCompletion() = runBlocking {
        val session = WakeSession(
            sessionId = "session-test-3",
            alarmId = 42L,
            targetSteps = 100,
            initialSensorSteps = 2000L,
            currentSteps = 100,
            status = AlarmStatus.CHALLENGE_COMPLETED,
            startedAtEpochMs = 1000L,
            startedRealtimeNs = 2000L
        )
        repository.startSession(session)

        val history = AlarmHistoryEntity(
            sessionId = "session-test-3",
            alarmId = 42L,
            scheduledTimeEpochMs = 1000L,
            triggeredAtEpochMs = 1000L,
            completedAtEpochMs = 3000L,
            status = AlarmStatus.CHALLENGE_COMPLETED,
            targetSteps = 100,
            completedSteps = 100,
            durationSeconds = 60L,
            emergencyDismissed = false
        )

        repository.completeSession(history)

        assertNull(repository.activeSessionFlow.value)
        assertNull(db.activeSessionDao().getActiveSession())

        val historyList = db.alarmHistoryDao().getHistoryFlow().first()
        assertEquals(1, historyList.size)
        assertEquals("session-test-3", historyList[0].sessionId)
    }
}
