package app.wakewalk.domain.model

data class ChallengeProgress(
    val current: Int,
    val target: Int,
    val fraction: Float,
    val isComplete: Boolean,
    val trackingMode: StepTrackingMode
)

data class WakeSession(
    val sessionId: String,
    val alarmId: Long,
    val targetSteps: Int,
    val initialSensorSteps: Long,
    val currentSteps: Int = 0,
    val status: AlarmStatus = AlarmStatus.CHALLENGE_ACTIVE,
    val startedAtEpochMs: Long,
    val startedRealtimeNs: Long,
    val completedAtEpochMs: Long? = null,
    val trackingMode: StepTrackingMode = StepTrackingMode.HARDWARE_COUNTER
) {
    val progress: ChallengeProgress
        get() = ChallengeProgress(
            current = currentSteps,
            target = targetSteps,
            fraction = if (targetSteps > 0) (currentSteps.toFloat() / targetSteps).coerceIn(0f, 1f) else 0f,
            isComplete = currentSteps >= targetSteps,
            trackingMode = trackingMode
        )

    val isComplete: Boolean
        get() = currentSteps >= targetSteps
}
