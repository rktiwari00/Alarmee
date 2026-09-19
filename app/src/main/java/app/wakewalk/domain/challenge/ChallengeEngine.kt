package app.wakewalk.domain.challenge

import app.wakewalk.domain.emergency.EmergencyDismissalAttempt
import app.wakewalk.domain.emergency.EmergencyDismissalPolicy
import app.wakewalk.domain.model.AlarmStatus
import app.wakewalk.domain.model.MotionSnapshot
import app.wakewalk.domain.model.StepInput
import app.wakewalk.domain.model.StepTrackingMode
import app.wakewalk.domain.model.WakeSession
import app.wakewalk.domain.movement.MovementValidator
import app.wakewalk.domain.movement.StepAccumulator
import app.wakewalk.domain.movement.StepValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChallengeEngine(
    private val validator: MovementValidator = MovementValidator(),
    private val emergencyPolicy: EmergencyDismissalPolicy = EmergencyDismissalPolicy()
) {
    private var accumulator: StepAccumulator = StepAccumulator()
    private val _sessionFlow = MutableStateFlow<WakeSession?>(null)
    val sessionFlow: StateFlow<WakeSession?> = _sessionFlow.asStateFlow()

    val currentSession: WakeSession?
        get() = _sessionFlow.value

    fun startSession(
        sessionId: String,
        alarmId: Long,
        targetSteps: Int,
        initialSensorSteps: Long,
        trackingMode: StepTrackingMode,
        startedAtEpochMs: Long,
        startedRealtimeNs: Long
    ) {
        accumulator = StepAccumulator(initialBaseline = initialSensorSteps)
        validator.reset()

        val session = WakeSession(
            sessionId = sessionId,
            alarmId = alarmId,
            targetSteps = targetSteps,
            initialSensorSteps = initialSensorSteps,
            currentSteps = 0,
            status = AlarmStatus.CHALLENGE_ACTIVE,
            startedAtEpochMs = startedAtEpochMs,
            startedRealtimeNs = startedRealtimeNs,
            trackingMode = trackingMode
        )
        _sessionFlow.value = session
    }

    fun restoreSession(session: WakeSession) {
        accumulator = StepAccumulator(
            initialBaseline = session.initialSensorSteps,
            initialRawSteps = session.currentSteps
        )
        validator.reset()
        _sessionFlow.value = session
    }

    fun processStep(input: StepInput, motion: MotionSnapshot?): StepValidationResult {
        val session = _sessionFlow.value ?: return StepValidationResult(accepted = false)
        if (session.status != AlarmStatus.CHALLENGE_ACTIVE && session.status != AlarmStatus.RINGING) {
            return StepValidationResult(accepted = false)
        }

        val timestampMs = when (input) {
            is StepInput.CounterUpdate -> input.timestampNs / 1_000_000L
            is StepInput.DetectorStep -> input.timestampNs / 1_000_000L
            is StepInput.AccelerometerCandidate -> input.timestampNs / 1_000_000L
        }

        val validation = validator.validate(timestampMs, motion)
        if (!validation.accepted) {
            return validation
        }

        val accumulated = accumulator.processInput(input)
        val newSteps = accumulated.coerceAtLeast(session.currentSteps)
        val isNowComplete = newSteps >= session.targetSteps

        val updatedSession = session.copy(
            currentSteps = newSteps,
            status = if (isNowComplete) AlarmStatus.CHALLENGE_COMPLETED else AlarmStatus.CHALLENGE_ACTIVE,
            completedAtEpochMs = if (isNowComplete) System.currentTimeMillis() else null
        )
        _sessionFlow.value = updatedSession

        return validation
    }

    fun attemptEmergencyDismissal(attempt: EmergencyDismissalAttempt, timestampEpochMs: Long): Boolean {
        val session = _sessionFlow.value ?: return false
        if (session.status == AlarmStatus.CHALLENGE_COMPLETED || session.status == AlarmStatus.DISMISSED) {
            return false
        }

        val isValid = emergencyPolicy.evaluate(attempt)
        if (isValid) {
            _sessionFlow.value = session.copy(
                status = AlarmStatus.EMERGENCY_DISMISSED,
                completedAtEpochMs = timestampEpochMs
            )
            return true
        }
        return false
    }

    fun snooze(timestampEpochMs: Long) {
        val session = _sessionFlow.value ?: return
        _sessionFlow.value = session.copy(
            status = AlarmStatus.SNOOZED,
            completedAtEpochMs = timestampEpochMs
        )
    }

    fun clearSession() {
        _sessionFlow.value = null
        validator.reset()
    }
}
