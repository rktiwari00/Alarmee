package app.wakewalk.domain.movement

/**
 * Anti-cheat validation configuration.
 *
 * All parameters represent tunable heuristics for distinguishing genuine
 * walking movement from violent shaking or resting vibration.
 */
data class ValidationConfig(
    val minStepIntervalMs: Long = 250L, // Flags cadence > 4.0 Hz (impossible human walking)
    val maxAcceptedStepIntervalMs: Long = 2500L, // Used in cadence evaluation
    val minimumWalkingConfidence: Float = 0.55f,
    val shakeConfidenceThreshold: Float = 0.70f,
    val severeShakeThresholdG: Float = 4.5f // Acceleration magnitude > 4.5G flags suspicious violent shaking
)
