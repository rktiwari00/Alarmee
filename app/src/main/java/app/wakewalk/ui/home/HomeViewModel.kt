package app.wakewalk.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.wakewalk.alarm.scheduler.AlarmScheduler
import app.wakewalk.data.local.entity.AlarmEntity
import app.wakewalk.domain.repository.AlarmRepository
import app.wakewalk.domain.repository.StatisticsRepository
import app.wakewalk.domain.scheduler.AlarmTimeCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val statisticsRepository: StatisticsRepository
) : ViewModel() {

    val alarms: StateFlow<List<AlarmEntity>> = alarmRepository.getAllAlarmsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _streakDays = MutableStateFlow(0)
    val streakDays: StateFlow<Int> = _streakDays.asStateFlow()

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
            alarmScheduler.cancelAlarm(alarm.id)
            alarmRepository.deleteAlarm(alarm)
        }
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
}
