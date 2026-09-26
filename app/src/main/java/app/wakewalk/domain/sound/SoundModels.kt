package app.wakewalk.domain.sound

enum class SoundCategory(val displayName: String) {
    HARSH("Harsh / Intense"),
    SMOOTH("Smooth / Gentle"),
    PHONE_CALL("Urgent Phone Call")
}

data class SoundItem(
    val id: String,
    val title: String,
    val category: SoundCategory,
    val rawResName: String? = null,
    val contentUri: String
)
