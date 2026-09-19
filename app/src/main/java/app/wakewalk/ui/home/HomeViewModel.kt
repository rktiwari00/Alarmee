package app.wakewalk.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.wakewalk.alarm.audio.AudioController
import app.wakewalk.alarm.scheduler.AlarmScheduler
import app.wakewalk.data.local.entity.AlarmEntity
import app.wakewalk.domain.repository.AlarmRepository
import app.wakewalk.domain.repository.StatisticsRepository
import app.wakewalk.domain.scheduler.AlarmTimeCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler,
    private val statisticsRepository: StatisticsRepository,
    private val audioController: AudioController
) : ViewModel() {

    val alarms: StateFlow<List<AlarmEntity>> = alarmRepository.getAllAlarmsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _streakDays = MutableStateFlow(0)
    val streakDays: StateFlow<Int> = _streakDays.asStateFlow()

    private val _playingAlarmId = MutableStateFlow<Long?>(null)
    val playingAlarmId: StateFlow<Long?> = _playingAlarmId.asStateFlow()

    private var previewJob: Job? = null

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            val stats = statisticsRepository.getStats()
            _streakDays.value = stats.currentStreakDays
        }
    }

    fun toggleAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            val isNowEnabled = alarmRepository.toggleAlarm(alarm)
            val updated = alarm.copy(isEnabled = isNowEnabled)
            if (isNowEnabled) {
                alarmScheduler.scheduleAlarm(updated)
            } else {
                alarmScheduler.cancelAlarm(alarm.id)
            }
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            if (_playingAlarmId.value == alarm.id) {
                stopSoundPreview()
            }
            alarmScheduler.cancelAlarm(alarm.id)
            alarmRepository.deleteAlarm(alarm)
        }
    }

    fun toggleSoundPreview(alarm: AlarmEntity) {
        if (_playingAlarmId.value == alarm.id) {
            stopSoundPreview()
        } else {
            stopSoundPreview()
            _playingAlarmId.value = alarm.id
            audioController.startAlarmAudio(alarm.soundUri, gradualVolume = false)
            previewJob = viewModelScope.launch {
                delay(5000L) // 5-second sample preview
                if (_playingAlarmId.value == alarm.id) {
                    stopSoundPreview()
                }
            }
        }
    }

    fun stopSoundPreview() {
        previewJob?.cancel()
        previewJob = null
        audioController.stopAudio()
        _playingAlarmId.value = null
    }

    fun getNextAlarmTimeRemaining(alarmList: List<AlarmEntity>): String? {
        val enabled = alarmList.filter { it.isEnabled }
        if (enabled.isEmpty()) return null

        val now = System.currentTimeMillis()
        val nextTimes = enabled.map {
            AlarmTimeCalculator.calculateNextTriggerTime(it.hour, it.minute, it.repeatDaysMask, now)
        }
        val earliest = nextTimes.minOrNull() ?: return null
        return AlarmTimeCalculator.formatTimeRemaining(earliest, now)
    }

    fun getNextAlarmMinuteOfDay(alarmList: List<AlarmEntity>): Int? {
        val enabled = alarmList.filter { it.isEnabled }
        if (enabled.isEmpty()) return null

        val now = System.currentTimeMillis()
        val nextWithAlarm = enabled.map { alarm ->
            alarm to AlarmTimeCalculator.calculateNextTriggerTime(alarm.hour, alarm.minute, alarm.repeatDaysMask, now)
        }.minByOrNull { it.second } ?: return null

        return nextWithAlarm.first.hour * 60 + nextWithAlarm.first.minute
    }

    override fun onCleared() {
        super.onCleared()
        audioController.stopAudio()
    }
}
