package app.wakewalk.domain.repository

import app.wakewalk.data.local.entity.AlarmHistoryEntity
import kotlinx.coroutines.flow.Flow

data class WakeStats(
    val completedCount: Int,
    val emergencyDismissedCount: Int,
    val missedCount: Int,
    val totalCount: Int,
    val successRate: Float,
    val averageDurationSeconds: Long,
    val totalStepsWalked: Long,
    val currentStreakDays: Int
)

interface StatisticsRepository {
    fun getHistoryFlow(): Flow<List<AlarmHistoryEntity>>
    fun getRecentHistoryFlow(limit: Int): Flow<List<AlarmHistoryEntity>>
    suspend fun getStats(): WakeStats
}
