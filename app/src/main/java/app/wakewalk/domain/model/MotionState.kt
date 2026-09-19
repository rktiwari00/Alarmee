package app.wakewalk.domain.model

enum class MotionState {
    WALKING,
    STATIONARY,
    SHAKING,
    UNKNOWN
}

data class MotionSnapshot(
    val timestampNs: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val magnitude: Float = kotlin.math.sqrt(x * x + y * y + z * z)
)

data class MotionAssessment(
    val state: MotionState,
    val confidence: Float,
    val timestampNs: Long
)
