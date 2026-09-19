package app.wakewalk.domain.movement

import app.wakewalk.domain.model.MotionSnapshot

enum class RejectionReason {
    TOO_FAST,
    SUSPICIOUS_MOTION,
    STATIONARY,
    INSUFFICIENT_EVIDENCE,
    DUPLICATE_EVENT
}

data class StepValidationResult(
    val accepted: Boolean,
    val reason: RejectionReason? = null,
    val confidence: Float = 1.0f
)

class MovementValidator(
    private val config: ValidationConfig = ValidationConfig()
) {
    private var lastStepTimestampMs: Long = 0L

    fun validate(timestampMs: Long, motion: MotionSnapshot?): StepValidationResult {
        // Check for rapid successive steps (impossible human gait cadence)
        if (lastStepTimestampMs > 0L) {
            val interval = timestampMs - lastStepTimestampMs
            if (interval < config.minStepIntervalMs) {
                return StepValidationResult(
                    accepted = false,
                    reason = RejectionReason.TOO_FAST,
                    confidence = 0.2f
                )
            }
        }

        // Check motion acceleration snapshot if available (anti-cheat against violent shaking)
        if (motion != null) {
            val magnitudeInG = motion.magnitude / 9.80665f
            if (magnitudeInG > config.severeShakeThresholdG) {
                return StepValidationResult(
                    accepted = false,
                    reason = RejectionReason.SUSPICIOUS_MOTION,
                    confidence = 0.15f
                )
            }
        }

        lastStepTimestampMs = timestampMs
        return StepValidationResult(
            accepted = true,
            confidence = 0.95f
        )
    }

    fun reset() {
        lastStepTimestampMs = 0L
    }
}
