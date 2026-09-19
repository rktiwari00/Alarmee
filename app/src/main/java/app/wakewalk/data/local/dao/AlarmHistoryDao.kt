package app.wakewalk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.wakewalk.data.local.entity.AlarmHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmHistoryDao {

    @Query("SELECT * FROM alarm_history ORDER BY completedAtEpochMs DESC")
    fun getHistoryFlow(): Flow<List<AlarmHistoryEntity>>

    @Query("SELECT * FROM alarm_history ORDER BY completedAtEpochMs DESC LIMIT :limit")
    fun getRecentHistoryFlow(limit: Int): Flow<List<AlarmHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: AlarmHistoryEntity): Long

    @Query("SELECT COUNT(*) FROM alarm_history WHERE status = 'CHALLENGE_COMPLETED'")
    suspend fun getCompletedCount(): Int

    @Query("SELECT COUNT(*) FROM alarm_history WHERE status = 'EMERGENCY_DISMISSED'")
    suspend fun getEmergencyDismissedCount(): Int

    @Query("SELECT COUNT(*) FROM alarm_history WHERE status = 'MISSED'")
    suspend fun getMissedCount(): Int

    @Query("SELECT COUNT(*) FROM alarm_history")
    suspend fun getTotalCount(): Int

    @Query("SELECT AVG(durationSeconds) FROM alarm_history WHERE status = 'CHALLENGE_COMPLETED'")
    suspend fun getAverageCompletionDurationSeconds(): Double?

    @Query("SELECT SUM(completedSteps) FROM alarm_history")
    suspend fun getTotalStepsWalked(): Long?
}
