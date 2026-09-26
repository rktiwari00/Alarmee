package app.wakewalk.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.wakewalk.alarm.audio.AudioController
import app.wakewalk.alarm.scheduler.AlarmScheduler
import app.wakewalk.data.local.entity.AlarmEntity
import app.wakewalk.domain.model.ChallengeType
import app.wakewalk.domain.repository.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

data class CreateEditAlarmUiState(
    val alarmId: Long = 0L,
    val hour: Int = 7,
    val minute: Int = 0,
    val label: String = "Wake up",
    val repeatDaysMask: Int = 0,
    val challengeType: ChallengeType = ChallengeType.WALK,
    val targetSteps: Int = 150,
    val qrCodePayload: String? = null,
    val qrCodeLabel: String? = null,
    val soundUri: String? = null,
    val soundTitle: String = AudioController.TITLE_PHONE_RINGTONE,
    val vibrationEnabled: Boolean = true,
    val gradualVolume: Boolean = true,
    val snoozeEnabled: Boolean = false,
    val isEditMode: Boolean = false,
    val isSaving: Boolean = false
)

@HiltViewModel
class CreateEditAlarmViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler,
    private val audioController: AudioController? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEditAlarmUiState())
    val uiState: StateFlow<CreateEditAlarmUiState> = _uiState.asStateFlow()

    fun initForAlarm(alarmId: Long) {
        if (alarmId > 0) {
            viewModelScope.launch {
                val alarm = alarmRepository.getAlarmById(alarmId) ?: return@launch
                val soundTitle = audioController?.getSoundTitle(alarm.soundUri) ?: when (alarm.soundUri) {
                    null, AudioController.URI_DEFAULT_CALL_RINGTONE -> AudioController.TITLE_PHONE_RINGTONE
                    AudioController.URI_DEFAULT_ALARM -> AudioController.TITLE_STANDARD_ALARM
                    else -> "Custom Sound"
                }
                _uiState.value = CreateEditAlarmUiState(
                    alarmId = alarm.id,
                    hour = alarm.hour,
                    minute = alarm.minute,
                    label = alarm.label,
                    repeatDaysMask = alarm.repeatDaysMask,
                    challengeType = alarm.challengeType,
                    targetSteps = alarm.targetSteps,
                    qrCodePayload = alarm.qrCodePayload,
                    qrCodeLabel = alarm.qrCodeLabel,
                    soundUri = alarm.soundUri,
                    soundTitle = soundTitle,
                    vibrationEnabled = alarm.vibrationEnabled,
                    gradualVolume = alarm.gradualVolume,
                    snoozeEnabled = alarm.snoozeEnabled,
                    isEditMode = true
                )
            }
        } else {
            // Default 7:00 AM
            val now = LocalTime.now()
            _uiState.value = CreateEditAlarmUiState(
                hour = 7,
                minute = 0,
                soundUri = null,
                soundTitle = AudioController.TITLE_PHONE_RINGTONE,
                isEditMode = false
            )
        }
    }

    fun setTime(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(hour = hour, minute = minute)
    }

    fun setLabel(label: String) {
        _uiState.value = _uiState.value.copy(label = label)
    }

    fun setRepeatDaysMask(mask: Int) {
        _uiState.value = _uiState.value.copy(repeatDaysMask = mask)
    }

    fun setChallengeType(type: ChallengeType) {
        val currentSteps = _uiState.value.targetSteps
        val adjustedSteps = when {
            type == ChallengeType.QR_CODE && currentSteps > 50 -> 15 // Default 15 steps for QR walk-to-target
            type == ChallengeType.WALK && currentSteps < 15 -> 150
            else -> currentSteps
        }
        _uiState.value = _uiState.value.copy(
            challengeType = type,
            targetSteps = adjustedSteps
        )
    }

    fun setQrCodeReference(payload: String, label: String?) {
        _uiState.value = _uiState.value.copy(
            qrCodePayload = payload,
            qrCodeLabel = label?.ifBlank { null }
        )
    }

    fun clearQrCodeReference() {
        _uiState.value = _uiState.value.copy(
            qrCodePayload = null,
            qrCodeLabel = null
        )
    }

    fun setTargetSteps(steps: Int) {
        _uiState.value = _uiState.value.copy(targetSteps = steps)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(vibrationEnabled = enabled)
    }

    fun setSoundUri(uri: String?, title: String? = null) {
        val resolvedTitle = title ?: audioController?.getSoundTitle(uri) ?: when (uri) {
            null, AudioController.URI_DEFAULT_CALL_RINGTONE -> AudioController.TITLE_PHONE_RINGTONE
            AudioController.URI_DEFAULT_ALARM -> AudioController.TITLE_STANDARD_ALARM
            else -> "Custom Sound"
        }
        _uiState.value = _uiState.value.copy(
            soundUri = uri,
            soundTitle = resolvedTitle
        )
    }

    fun setGradualVolume(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(gradualVolume = enabled)
    }

    fun setSnoozeEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(snoozeEnabled = enabled)
    }

    fun isSaveAllowed(): Boolean {
        val state = _uiState.value
        return state.challengeType != ChallengeType.QR_CODE || !state.qrCodePayload.isNullOrBlank()
    }

    fun saveAlarm(onSuccess: () -> Unit) {
        if (!isSaveAllowed()) return
        val state = _uiState.value
        viewModelScope.launch {
            val alarmEntity = AlarmEntity(
                id = state.alarmId,
                hour = state.hour,
                minute = state.minute,
                label = state.label.ifBlank { "Wake up" },
                isEnabled = true,
                repeatDaysMask = state.repeatDaysMask,
                challengeType = state.challengeType,
                targetSteps = state.targetSteps,
                qrCodePayload = state.qrCodePayload,
                qrCodeLabel = state.qrCodeLabel,
                soundUri = state.soundUri,
                vibrationEnabled = state.vibrationEnabled,
                gradualVolume = state.gradualVolume,
                snoozeEnabled = state.snoozeEnabled,
                updatedAtEpochMs = System.currentTimeMillis()
            )

            val id = if (state.isEditMode) {
                alarmRepository.updateAlarm(alarmEntity)
                state.alarmId
            } else {
                alarmRepository.insertAlarm(alarmEntity)
            }

            // Schedule with AlarmManager
            alarmScheduler.scheduleAlarm(alarmEntity.copy(id = id))
            onSuccess()
        }
    }

    fun deleteAlarm(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.isEditMode) {
            viewModelScope.launch {
                alarmScheduler.cancelAlarm(state.alarmId)
                val alarm = alarmRepository.getAlarmById(state.alarmId)
                if (alarm != null) {
                    alarmRepository.deleteAlarm(alarm)
                }
                onSuccess()
            }
        }
    }
}
