package app.wakewalk.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.wakewalk.data.local.entity.ActiveSessionEntity
import app.wakewalk.data.local.entity.AlarmEntity
import app.wakewalk.data.local.entity.AlarmHistoryEntity
import app.wakewalk.domain.model.AlarmStatus
import app.wakewalk.domain.model.ChallengeType
import app.wakewalk.domain.model.StepTrackingMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RoomDatabaseTest {

    private lateinit var db: WakeWalkDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WakeWalkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testAlarmCrud() = runBlocking {
        val alarm = AlarmEntity(
            hour = 7,
            minute = 0,
            label = "Morning Run",
            isEnabled = true,
            repeatDaysMask = 31, // Weekdays
            challengeType = ChallengeType.WALK,
            targetSteps = 200
        )
        val id = db.alarmDao().insertAlarm(alarm)
        assertTrue(id > 0)

        val retrieved = db.alarmDao().getAlarmById(id)
        assertNotNull(retrieved)
        assertEquals("Morning Run", retrieved!!.label)
        assertEquals(200, retrieved.targetSteps)
        assertEquals(31, retrieved.repeatDaysMask)

        val updated = retrieved.copy(targetSteps = 250, isEnabled = false)
        db.alarmDao().updateAlarm(updated)
        val afterUpdate = db.alarmDao().getAlarmById(id)
        assertEquals(250, afterUpdate!!.targetSteps)
        assertEquals(false, afterUpdate.isEnabled)

        val alarmsList = db.alarmDao().getAllAlarmsFlow().first()
        assertEquals(1, alarmsList.size)

        db.alarmDao().deleteAlarm(afterUpdate)
        val afterDelete = db.alarmDao().getAlarmById(id)
        assertNull(afterDelete)
    }

    @Test
    fun testActiveSessionSingletonAndAtomicCompletion() = runBlocking {
        val session = ActiveSessionEntity(
            sessionId = "session-123",
            alarmId = 1L,
            targetSteps = 150,
            initialSensorSteps = 5000L,
            currentSteps = 73,
            startedAtEpochMs = 1000L,
            startedRealtimeNs = 2000L,
            status = AlarmStatus.CHALLENGE_ACTIVE,
            trackingMode = StepTrackingMode.HARDWARE_COUNTER,
            lastUpdatedEpochMs = 3000L
        )

        db.activeSessionDao().upsertSession(session)
        val stored = db.activeSessionDao().getActiveSession()
        assertNotNull(stored)
        assertEquals(73, stored!!.currentSteps)
        assertEquals("session-123", stored.sessionId)

        // Test atomic completion transaction
        val history = AlarmHistoryEntity(
            sessionId = "session-123",
            alarmId = 1L,
            scheduledTimeEpochMs = 1000L,
            triggeredAtEpochMs = 1000L,
            completedAtEpochMs = 4000L,
            status = AlarmStatus.CHALLENGE_COMPLETED,
            targetSteps = 150,
            completedSteps = 150,
            durationSeconds = 120L,
            emergencyDismissed = false
        )

        db.completeActiveSession(history)

        // Verify active session was cleared
        val activeAfter = db.activeSessionDao().getActiveSession()
        assertNull(activeAfter)

        // Verify history was inserted
        val historyList = db.alarmHistoryDao().getHistoryFlow().first()
        assertEquals(1, historyList.size)
        assertEquals("session-123", historyList[0].sessionId)
        assertEquals(AlarmStatus.CHALLENGE_COMPLETED, historyList[0].status)
    }
}
