package app.wakewalk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.wakewalk.domain.model.AlarmStatus
import app.wakewalk.domain.model.StepTrackingMode

@Entity(tableName = "active_session")
data class ActiveSessionEntity(
    @PrimaryKey val singletonId: Int = 1, // Enforces maximum one active session policy
    val sessionId: String,
    val alarmId: Long,
    val targetSteps: Int,
    val initialSensorSteps: Long,
    val currentSteps: Int,
    val startedAtEpochMs: Long,
    val startedRealtimeNs: Long,
    val status: AlarmStatus,
    val trackingMode: StepTrackingMode,
    val lastUpdatedEpochMs: Long
)
