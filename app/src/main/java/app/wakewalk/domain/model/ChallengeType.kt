package app.wakewalk.domain.model

enum class ChallengeType {
    WALK,
    QR_CODE,
    DISTANCE,       // Extensible for v2
    STAY_ACTIVE     // Extensible for v2
}
