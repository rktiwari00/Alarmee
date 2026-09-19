package app.wakewalk.domain.model

enum class AlarmStatus {
    SCHEDULED,
    TRIGGERED,
    RINGING,
    CHALLENGE_ACTIVE,
    CHALLENGE_COMPLETED,
    SNOOZED,
    MISSED,
    DISMISSED,
    EMERGENCY_DISMISSED
}
