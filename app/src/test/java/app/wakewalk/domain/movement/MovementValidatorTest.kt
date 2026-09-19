package app.wakewalk.domain.movement

import app.wakewalk.domain.model.MotionSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MovementValidatorTest {

    private val validator = MovementValidator(ValidationConfig(minStepIntervalMs = 320L))

    @Test
    fun testNormalWalkingStepsAccepted() {
        val result1 = validator.validate(timestampMs = 1000L, motion = MotionSnapshot(1000L, 0.2f, 1.1f, 0.3f))
        assertTrue(result1.accepted)

        // Step 600ms later (typical 1.6 Hz gait)
        val result2 = validator.validate(timestampMs = 1600L, motion = MotionSnapshot(1600L, 0.3f, 1.2f, 0.2f))
        assertTrue(result2.accepted)
    }

    @Test
    fun testRapidShakingRejected() {
        val result1 = validator.validate(timestampMs = 1000L, motion = null)
        assertTrue(result1.accepted)

        // Rapid second step only 150ms later (>6 Hz frequency, impossible human walking)
        val result2 = validator.validate(timestampMs = 1150L, motion = null)
        assertFalse(result2.accepted)
        assertEquals(RejectionReason.TOO_FAST, result2.reason)
    }

    @Test
    fun testViolentAccelerationRejected() {
        val violentMotion = MotionSnapshot(
            timestampNs = 1000L,
            x = 3.5f * 9.8f,
            y = 2.0f * 9.8f,
            z = 4.0f * 9.8f
        ) // > 5G multi-axis violent shake
        val result = validator.validate(timestampMs = 1000L, motion = violentMotion)
        assertFalse(result.accepted)
        assertEquals(RejectionReason.SUSPICIOUS_MOTION, result.reason)
    }
}
