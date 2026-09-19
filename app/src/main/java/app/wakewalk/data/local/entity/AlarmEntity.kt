package app.wakewalk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.wakewalk.domain.model.ChallengeType

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String,
    val isEnabled: Boolean,
    val repeatDaysMask: Int, // 7-bit mask: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64. 0 = Once
    val challengeType: ChallengeType = ChallengeType.WALK,
    val targetSteps: Int = 150,
    val soundUri: String? = null,
    val vibrationEnabled: Boolean = true,
    val gradualVolume: Boolean = true,
    val snoozeEnabled: Boolean = false,
    val snoozeDurationMinutes: Int = 5,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)
