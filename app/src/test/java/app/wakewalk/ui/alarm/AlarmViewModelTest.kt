package app.wakewalk.ui.alarm

import app.wakewalk.domain.model.AlarmStatus
import app.wakewalk.domain.model.ChallengeType
import app.wakewalk.domain.model.StepTrackingMode
import app.wakewalk.domain.model.WakeSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmViewModelTest {

    @Test
    fun testQrScanRejectedIfStepsBelowTarget() {
        val targetCode = "TARGET_BARCODE_BATHROOM"
        val state = AlarmUiState(
            session = WakeSession(
                sessionId = "s1",
                alarmId = 1L,
                targetSteps = 15,
                initialSensorSteps = 0L,
                currentSteps = 5,
                status = AlarmStatus.CHALLENGE_ACTIVE,
                startedAtEpochMs = 1000L,
                startedRealtimeNs = 1000L,
                trackingMode = StepTrackingMode.HARDWARE_DETECTOR
            ),
            targetSteps = 15,
            completedSteps = 5,
            targetQrPayload = targetCode
        )

        val eval = AlarmViewModel.evaluateQrCodeScan(
            scannedCode = targetCode,
            targetPayload = state.targetQrPayload,
            currentSteps = state.completedSteps,
            targetSteps = state.targetSteps
        )

        assertFalse(eval.isComplete)
        assertEquals("You're still in bed! Walk 10 more steps before scanning.", eval.errorMessage)
    }

    @Test
    fun testQrScanAcceptedWhenStepsReachedAndCodeMatches() {
        val targetCode = "TARGET_BARCODE_BATHROOM"
        val eval = AlarmViewModel.evaluateQrCodeScan(
            scannedCode = targetCode,
            targetPayload = targetCode,
            currentSteps = 15,
            targetSteps = 15
        )

        assertTrue(eval.isComplete)
        assertEquals(null, eval.errorMessage)
    }

    @Test
    fun testQrScanRejectedWhenCodeMismatches() {
        val eval = AlarmViewModel.evaluateQrCodeScan(
            scannedCode = "WRONG_BARCODE",
            targetPayload = "TARGET_BARCODE_BATHROOM",
            currentSteps = 20,
            targetSteps = 15
        )

        assertFalse(eval.isComplete)
        assertEquals("Wrong code scanned! Look for your registered item.", eval.errorMessage)
    }
}
