package app.wakewalk.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.wakewalk.data.local.WakeWalkDatabase
import app.wakewalk.data.local.entity.AlarmHistoryEntity
import app.wakewalk.domain.model.AlarmStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class StatisticsRepositoryTest {

    private lateinit var db: WakeWalkDatabase
    private lateinit var repository: StatisticsRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WakeWalkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = StatisticsRepositoryImpl(db.alarmHistoryDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testStatisticsDistinguishesSuccessFromEmergencyDismissal() = runBlocking {
        // Insert 2 completed alarms
        db.alarmHistoryDao().insertHistory(
            AlarmHistoryEntity(
                sessionId = "s1",
                alarmId = 1L,
                scheduledTimeEpochMs = 1000L,
                triggeredAtEpochMs = 1000L,
                completedAtEpochMs = 2000L,
                status = AlarmStatus.CHALLENGE_COMPLETED,
                targetSteps = 100,
                completedSteps = 100,
                durationSeconds = 60L,
                emergencyDismissed = false
            )
        )
        db.alarmHistoryDao().insertHistory(
            AlarmHistoryEntity(
                sessionId = "s2",
                alarmId = 1L,
                scheduledTimeEpochMs = 3000L,
                triggeredAtEpochMs = 3000L,
                completedAtEpochMs = 4000L,
                status = AlarmStatus.CHALLENGE_COMPLETED,
                targetSteps = 100,
                completedSteps = 100,
                durationSeconds = 120L,
                emergencyDismissed = false
            )
        )

        // Insert 1 emergency dismissed alarm
        db.alarmHistoryDao().insertHistory(
            AlarmHistoryEntity(
                sessionId = "s3",
                alarmId = 1L,
                scheduledTimeEpochMs = 5000L,
                triggeredAtEpochMs = 5000L,
                completedAtEpochMs = 5030L,
                status = AlarmStatus.EMERGENCY_DISMISSED,
                targetSteps = 100,
                completedSteps = 10,
                durationSeconds = 30L,
                emergencyDismissed = true
            )
        )

        val stats = repository.getStats()
        assertEquals(2, stats.completedCount)
        assertEquals(1, stats.emergencyDismissedCount)
        assertEquals(3, stats.totalCount)
        // Success rate should be 2 / 3 = 66.7%, emergency dismissal is NOT counted as success
        assertEquals(2f / 3f, stats.successRate, 0.01f)
        assertEquals(90L, stats.averageDurationSeconds) // (60 + 120) / 2
        assertEquals(210L, stats.totalStepsWalked) // 100 + 100 + 10
    }
}
