package app.wakewalk.ui.alarm

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.wakewalk.alarm.service.AlarmForegroundService
import app.wakewalk.data.preferences.UserPreferences
import app.wakewalk.data.preferences.UserPreferencesRepository
import app.wakewalk.domain.emergency.EmergencyDismissalAttempt
import app.wakewalk.domain.emergency.EmergencyDismissalPolicy
import app.wakewalk.domain.model.AlarmStatus
import app.wakewalk.domain.model.WakeSession
import app.wakewalk.domain.repository.WakeSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlarmUiState(
    val session: WakeSession? = null,
    val isCompleted: Boolean = false,
    val completedSteps: Int = 0,
    val targetSteps: Int = 150,
    val durationSeconds: Long = 0L,
    val isEmergencyDismissed: Boolean = false,
    val emergencyDialogVisible: Boolean = false,
    val emergencyPhraseInput: String = "",
    val longPressDurationMs: Long = 0L
)

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val wakeSessionRepository: WakeSessionRepository,
    private val preferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val emergencyPolicy = EmergencyDismissalPolicy()

    val preferencesFlow: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    private val _uiState = MutableStateFlow(AlarmUiState())
    val uiState: StateFlow<AlarmUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Restore session if needed
            wakeSessionRepository.restoreSessionFromDb()

            wakeSessionRepository.activeSessionFlow.collect { session ->
                if (session != null) {
                    val wasCompleted = session.status == AlarmStatus.CHALLENGE_COMPLETED ||
                            session.currentSteps >= session.targetSteps
                    val wasEmergency = session.status == AlarmStatus.EMERGENCY_DISMISSED
                    val duration = if (session.startedAtEpochMs > 0) {
                        (System.currentTimeMillis() - session.startedAtEpochMs) / 1000L
                    } else {
                        0L
                    }

                    _uiState.value = _uiState.value.copy(
                        session = session,
                        isCompleted = wasCompleted,
                        completedSteps = session.currentSteps,
                        targetSteps = session.targetSteps,
                        durationSeconds = duration,
                        isEmergencyDismissed = wasEmergency
                    )
                } else {
                    // If session cleared while completed, retain completed state for result card
                    if (!_uiState.value.isCompleted && !_uiState.value.isEmergencyDismissed) {
                        _uiState.value = _uiState.value.copy(session = null)
                    }
                }
            }
        }
    }

    fun showEmergencyDialog() {
        _uiState.value = _uiState.value.copy(
            emergencyDialogVisible = true,
            emergencyPhraseInput = "",
            longPressDurationMs = 0L
        )
    }

    fun dismissEmergencyDialog() {
        _uiState.value = _uiState.value.copy(
            emergencyDialogVisible = false,
            emergencyPhraseInput = "",
            longPressDurationMs = 0L
        )
    }

    fun onEmergencyPhraseChanged(phrase: String) {
        _uiState.value = _uiState.value.copy(emergencyPhraseInput = phrase)
    }

    fun isPhraseValid(): Boolean {
        return emergencyPolicy.evaluate(
            EmergencyDismissalAttempt.PhraseConfirmed(_uiState.value.emergencyPhraseInput)
        )
    }

    fun submitEmergencyPhrase() {
        if (isPhraseValid()) {
            sendServiceAction(AlarmForegroundService.ACTION_EMERGENCY_DISMISS)
            _uiState.value = _uiState.value.copy(
                emergencyDialogVisible = false,
                isEmergencyDismissed = true
            )
        }
    }

    fun updateLongPressDuration(durationMs: Long) {
        _uiState.value = _uiState.value.copy(longPressDurationMs = durationMs)
        if (emergencyPolicy.evaluate(EmergencyDismissalAttempt.LongPressCompleted(durationMs))) {
            sendServiceAction(AlarmForegroundService.ACTION_EMERGENCY_DISMISS)
            _uiState.value = _uiState.value.copy(
                emergencyDialogVisible = false,
                isEmergencyDismissed = true
            )
        }
    }

    fun snooze() {
        sendServiceAction(AlarmForegroundService.ACTION_SNOOZE)
    }

    private fun sendServiceAction(action: String) {
        val intent = Intent(context, AlarmForegroundService::class.java).apply {
            this.action = action
        }
        context.startService(intent)
    }
}
