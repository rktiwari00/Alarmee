package app.wakewalk.domain.model

enum class StepTrackingMode {
    HARDWARE_COUNTER,
    HARDWARE_DETECTOR,
    ACCELEROMETER_FALLBACK,
    UNAVAILABLE
}
