package app.wakewalk.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.wakewalk.data.local.entity.AlarmHistoryEntity
import app.wakewalk.domain.repository.StatisticsRepository
import app.wakewalk.domain.repository.WakeStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val statisticsRepository: StatisticsRepository
) : ViewModel() {

    private val _stats = MutableStateFlow(
        WakeStats(
            completedCount = 0,
            emergencyDismissedCount = 0,
            missedCount = 0,
            totalCount = 0,
            successRate = 0f,
            averageDurationSeconds = 0L,
            totalStepsWalked = 0L,
            currentStreakDays = 0
        )
    )
    val stats: StateFlow<WakeStats> = _stats.asStateFlow()

    val historyList: StateFlow<List<AlarmHistoryEntity>> = statisticsRepository.getHistoryFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _stats.value = statisticsRepository.getStats()
        }
    }
}
