package app.wakewalk.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val targetSteps: Int = 150,
    val vibrationEnabled: Boolean = true,
    val gradualVolume: Boolean = true,
    val snoozeEnabled: Boolean = false,
    val isEditMode: Boolean = false,
    val isSaving: Boolean = false
)

@HiltViewModel
class CreateEditAlarmViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEditAlarmUiState())
    val uiState: StateFlow<CreateEditAlarmUiState> = _uiState.asStateFlow()

    fun initForAlarm(alarmId: Long) {
        if (alarmId > 0) {
            viewModelScope.launch {
                val alarm = alarmRepository.getAlarmById(alarmId) ?: return@launch
                _uiState.value = CreateEditAlarmUiState(
                    alarmId = alarm.id,
                    hour = alarm.hour,
                    minute = alarm.minute,
                    label = alarm.label,
                    repeatDaysMask = alarm.repeatDaysMask,
                    targetSteps = alarm.targetSteps,
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

    fun setTargetSteps(steps: Int) {
        _uiState.value = _uiState.value.copy(targetSteps = steps)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(vibrationEnabled = enabled)
    }

    fun setGradualVolume(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(gradualVolume = enabled)
    }

    fun setSnoozeEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(snoozeEnabled = enabled)
    }

    fun saveAlarm(onSuccess: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            val alarmEntity = AlarmEntity(
                id = state.alarmId,
                hour = state.hour,
                minute = state.minute,
                label = state.label.ifBlank { "Wake up" },
                isEnabled = true,
                repeatDaysMask = state.repeatDaysMask,
                challengeType = ChallengeType.WALK,
                targetSteps = state.targetSteps,
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
