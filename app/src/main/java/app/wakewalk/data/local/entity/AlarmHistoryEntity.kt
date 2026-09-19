package app.wakewalk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.wakewalk.domain.model.AlarmStatus

@Entity(tableName = "alarm_history")
data class AlarmHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val alarmId: Long,
    val scheduledTimeEpochMs: Long,
    val triggeredAtEpochMs: Long?,
    val completedAtEpochMs: Long?,
    val status: AlarmStatus,
    val targetSteps: Int,
    val completedSteps: Int,
    val durationSeconds: Long?,
    val emergencyDismissed: Boolean
)
