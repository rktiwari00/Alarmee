package app.wakewalk.domain.emergency

sealed interface EmergencyDismissalAttempt {
    data class PhraseConfirmed(val phrase: String) : EmergencyDismissalAttempt
    data class LongPressCompleted(val durationMs: Long) : EmergencyDismissalAttempt
}

class EmergencyDismissalPolicy(
    val expectedPhrase: String = "I AM AWAKE",
    val requiredLongPressMs: Long = 5000L
) {
    fun evaluate(attempt: EmergencyDismissalAttempt): Boolean {
        return when (attempt) {
            is EmergencyDismissalAttempt.PhraseConfirmed -> {
                attempt.phrase.trim().equals(expectedPhrase, ignoreCase = true)
            }
            is EmergencyDismissalAttempt.LongPressCompleted -> {
                attempt.durationMs >= requiredLongPressMs
            }
        }
    }
}
