package app.wakewalk.domain.movement

/**
 * Anti-cheat validation configuration.
 *
 * All parameters represent initial tunable heuristics for distinguishing genuine
 * walking movement from shaking or resting vibration, and are open for calibration.
 */
data class ValidationConfig(
    val minStepIntervalMs: Long = 320L, // Initial heuristic: flags cadence > 3.1 Hz
    val maxAcceptedStepIntervalMs: Long = 2500L, // Used in cadence evaluation; does not discard steps after pauses
    val minimumWalkingConfidence: Float = 0.55f,
    val shakeConfidenceThreshold: Float = 0.70f,
    val severeShakeThresholdG: Float = 3.2f // Accelerometer magnitude > 3.2G flags suspicious motion
)
