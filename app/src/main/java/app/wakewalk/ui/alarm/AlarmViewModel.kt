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
import app.wakewalk.domain.model.ChallengeType
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

import app.wakewalk.alarm.audio.AudioController
import app.wakewalk.alarm.audio.VolumeDuckingState

data class QrScanEvaluation(
    val isComplete: Boolean,
    val errorMessage: String? = null
)

data class AlarmUiState(
    val session: WakeSession? = null,
    val isCompleted: Boolean = false,
    val completedSteps: Int = 0,
    val targetSteps: Int = 150,
    val challengeType: ChallengeType = ChallengeType.WALK,
    val targetQrPayload: String? = null,
    val targetQrLabel: String? = null,
    val qrScanError: String? = null,
    val isTorchEnabled: Boolean = false,
    val durationSeconds: Long = 0L,
    val isEmergencyDismissed: Boolean = false,
    val emergencyDialogVisible: Boolean = false,
    val emergencyPhraseInput: String = "",
    val longPressDurationMs: Long = 0L,
    val volumeDuckingState: VolumeDuckingState = VolumeDuckingState.FULL_VOLUME
)

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val wakeSessionRepository: WakeSessionRepository,
    private val preferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context,
    val audioController: AudioController? = null
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
        if (audioController != null) {
            viewModelScope.launch {
                audioController.volumeDuckingState.collect { duckState ->
                    _uiState.value = _uiState.value.copy(volumeDuckingState = duckState)
                }
            }
        }

        viewModelScope.launch {
            // Restore session if needed
            wakeSessionRepository.restoreSessionFromDb()

            wakeSessionRepository.activeSessionFlow.collect { session ->
                if (session != null) {
                    val wasCompleted = session.status == AlarmStatus.CHALLENGE_COMPLETED ||
                            (session.status == AlarmStatus.CHALLENGE_ACTIVE &&
                                    _uiState.value.challengeType == ChallengeType.WALK &&
                                    session.currentSteps >= session.targetSteps)
                    val wasEmergency = session.status == AlarmStatus.EMERGENCY_DISMISSED
                    val duration = if (session.startedAtEpochMs > 0) {
                        (System.currentTimeMillis() - session.startedAtEpochMs) / 1000L
                    } else {
                        0L
                    }

                    _uiState.value = _uiState.value.copy(
                        session = session,
                        isCompleted = wasCompleted || _uiState.value.isCompleted,
                        completedSteps = session.currentSteps,
                        targetSteps = if (_uiState.value.targetSteps > 0 && _uiState.value.challengeType == ChallengeType.QR_CODE) {
                            _uiState.value.targetSteps
                        } else {
                            session.targetSteps
                        },
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

    fun initChallengeData(
        type: ChallengeType,
        targetSteps: Int,
        qrPayload: String?,
        qrLabel: String?
    ) {
        _uiState.value = _uiState.value.copy(
            challengeType = type,
            targetSteps = if (targetSteps > 0) targetSteps else _uiState.value.targetSteps,
            targetQrPayload = qrPayload,
            targetQrLabel = qrLabel
        )
    }

    fun onBarcodeScanned(scannedCode: String) {
        if (_uiState.value.isCompleted) return
        val currentState = _uiState.value
        val eval = evaluateQrCodeScan(
            scannedCode = scannedCode,
            targetPayload = currentState.targetQrPayload,
            currentSteps = currentState.completedSteps,
            targetSteps = currentState.targetSteps
        )
        if (eval.isComplete) {
            _uiState.value = _uiState.value.copy(
                isCompleted = true,
                qrScanError = null
            )
            sendServiceAction(AlarmForegroundService.ACTION_COMPLETE_CHALLENGE)
        } else {
            _uiState.value = _uiState.value.copy(
                qrScanError = eval.errorMessage
            )
        }
    }

    fun toggleTorch() {
        _uiState.value = _uiState.value.copy(
            isTorchEnabled = !_uiState.value.isTorchEnabled
        )
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

    companion object {
        fun evaluateQrCodeScan(
            scannedCode: String,
            targetPayload: String?,
            currentSteps: Int,
            targetSteps: Int
        ): QrScanEvaluation {
            if (currentSteps < targetSteps) {
                val remaining = targetSteps - currentSteps
                return QrScanEvaluation(
                    isComplete = false,
                    errorMessage = "You're still in bed! Walk $remaining more steps before scanning."
                )
            }
            if (targetPayload.isNullOrBlank() || scannedCode != targetPayload) {
                return QrScanEvaluation(
                    isComplete = false,
                    errorMessage = "Wrong code scanned! Look for your registered item."
                )
            }
            return QrScanEvaluation(isComplete = true)
        }
    }
}
