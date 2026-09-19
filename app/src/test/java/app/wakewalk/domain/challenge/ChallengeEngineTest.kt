package app.wakewalk.domain.challenge

import app.wakewalk.domain.emergency.EmergencyDismissalAttempt
import app.wakewalk.domain.emergency.EmergencyDismissalPolicy
import app.wakewalk.domain.model.AlarmStatus
import app.wakewalk.domain.model.StepInput
import app.wakewalk.domain.model.StepTrackingMode
import app.wakewalk.domain.movement.MovementValidator
import app.wakewalk.domain.movement.ValidationConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChallengeEngineTest {

    private lateinit var engine: ChallengeEngine

    @Before
    fun setup() {
        val validator = MovementValidator(ValidationConfig(minStepIntervalMs = 300L))
        val policy = EmergencyDismissalPolicy(expectedPhrase = "I AM AWAKE", requiredLongPressMs = 5000L)
        engine = ChallengeEngine(validator, policy)
    }

    @Test
    fun testChallengeProgressionAndCompletion() {
        engine.startSession(
            sessionId = "test-session-1",
            alarmId = 1L,
            targetSteps = 3,
            initialSensorSteps = 100L,
            trackingMode = StepTrackingMode.HARDWARE_DETECTOR,
            startedAtEpochMs = 1000L,
            startedRealtimeNs = 1000L
        )

        val initialSession = engine.currentSession
        assertNotNull(initialSession)
        assertEquals(0, initialSession!!.currentSteps)
        assertEquals(AlarmStatus.CHALLENGE_ACTIVE, initialSession.status)

        // Step 1
        val res1 = engine.processStep(StepInput.DetectorStep(1500_000_000L), motion = null)
        assertTrue(res1.accepted)
        assertEquals(1, engine.currentSession!!.currentSteps)
        assertFalse(engine.currentSession!!.isComplete)

        // Step 2
        val res2 = engine.processStep(StepInput.DetectorStep(2100_000_000L), motion = null)
        assertTrue(res2.accepted)
        assertEquals(2, engine.currentSession!!.currentSteps)

        // Step 3 (Completion target reached)
        val res3 = engine.processStep(StepInput.DetectorStep(2700_000_000L), motion = null)
        assertTrue(res3.accepted)
        assertEquals(3, engine.currentSession!!.currentSteps)
        assertTrue(engine.currentSession!!.isComplete)
        assertEquals(AlarmStatus.CHALLENGE_COMPLETED, engine.currentSession!!.status)
        assertNotNull(engine.currentSession!!.completedAtEpochMs)
    }

    @Test
    fun testEmergencyDismissalTypedPhrase() {
        engine.startSession(
            sessionId = "test-session-2",
            alarmId = 2L,
            targetSteps = 50,
            initialSensorSteps = 0L,
            trackingMode = StepTrackingMode.HARDWARE_DETECTOR,
            startedAtEpochMs = 1000L,
            startedRealtimeNs = 1000L
        )

        // Wrong phrase
        val failed = engine.attemptEmergencyDismissal(
            EmergencyDismissalAttempt.PhraseConfirmed("i am sleepy"),
            timestampEpochMs = 2000L
        )
        assertFalse(failed)
        assertEquals(AlarmStatus.CHALLENGE_ACTIVE, engine.currentSession!!.status)

        // Correct phrase (case-insensitive)
        val success = engine.attemptEmergencyDismissal(
            EmergencyDismissalAttempt.PhraseConfirmed("i am awake"),
            timestampEpochMs = 2500L
        )
        assertTrue(success)
        assertEquals(AlarmStatus.EMERGENCY_DISMISSED, engine.currentSession!!.status)
    }

    @Test
    fun testEmergencyDismissalLongPress() {
        engine.startSession(
            sessionId = "test-session-3",
            alarmId = 3L,
            targetSteps = 50,
            initialSensorSteps = 0L,
            trackingMode = StepTrackingMode.HARDWARE_DETECTOR,
            startedAtEpochMs = 1000L,
            startedRealtimeNs = 1000L
        )

        // Short hold
        val failed = engine.attemptEmergencyDismissal(
            EmergencyDismissalAttempt.LongPressCompleted(3500L),
            timestampEpochMs = 5000L
        )
        assertFalse(failed)

        // Full 5 second hold
        val success = engine.attemptEmergencyDismissal(
            EmergencyDismissalAttempt.LongPressCompleted(5000L),
            timestampEpochMs = 7000L
        )
        assertTrue(success)
        assertEquals(AlarmStatus.EMERGENCY_DISMISSED, engine.currentSession!!.status)
    }
}
