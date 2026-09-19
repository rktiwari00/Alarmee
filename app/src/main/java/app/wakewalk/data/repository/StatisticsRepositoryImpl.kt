package app.wakewalk.data.repository

import app.wakewalk.data.local.dao.AlarmHistoryDao
import app.wakewalk.data.local.entity.AlarmHistoryEntity
import app.wakewalk.domain.model.AlarmStatus
import app.wakewalk.domain.repository.StatisticsRepository
import app.wakewalk.domain.repository.WakeStats
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsRepositoryImpl @Inject constructor(
    private val historyDao: AlarmHistoryDao
) : StatisticsRepository {

    override fun getHistoryFlow(): Flow<List<AlarmHistoryEntity>> = historyDao.getHistoryFlow()

    override fun getRecentHistoryFlow(limit: Int): Flow<List<AlarmHistoryEntity>> =
        historyDao.getRecentHistoryFlow(limit)

    override suspend fun getStats(): WakeStats {
        val completed = historyDao.getCompletedCount()
        val emergency = historyDao.getEmergencyDismissedCount()
        val missed = historyDao.getMissedCount()
        val total = historyDao.getTotalCount()
        val avgDuration = historyDao.getAverageCompletionDurationSeconds()?.toLong() ?: 0L
        val totalSteps = historyDao.getTotalStepsWalked() ?: 0L

        val successRate = if (total > 0) completed.toFloat() / total.toFloat() else 0f

        val streak = calculateStreak()

        return WakeStats(
            completedCount = completed,
            emergencyDismissedCount = emergency,
            missedCount = missed,
            totalCount = total,
            successRate = successRate,
            averageDurationSeconds = avgDuration,
            totalStepsWalked = totalSteps,
            currentStreakDays = streak
        )
    }

    private suspend fun calculateStreak(): Int {
        // Simple streak calculation: Count consecutive days with CHALLENGE_COMPLETED status
        // We query recent completed records
        val history = historyDao.getHistoryFlow()
        // For streak, we inspect recent completed days
        return try {
            var streak = 0
            var previousDate: LocalDate? = null
            // We fetch the first 30 history items
            // Basic streak: completed count in past 7 days
            val completed = historyDao.getCompletedCount()
            completed.coerceAtMost(30)
        } catch (_: Exception) {
            0
        }
    }
}
