package app.wakewalk.data.local.converter

import androidx.room.TypeConverter
import app.wakewalk.domain.model.AlarmStatus
import app.wakewalk.domain.model.ChallengeType
import app.wakewalk.domain.model.StepTrackingMode

class Converters {
    @TypeConverter
    fun fromAlarmStatus(status: AlarmStatus): String = status.name

    @TypeConverter
    fun toAlarmStatus(value: String): AlarmStatus = runCatching {
        AlarmStatus.valueOf(value)
    }.getOrDefault(AlarmStatus.RINGING)

    @TypeConverter
    fun fromChallengeType(type: ChallengeType): String = type.name

    @TypeConverter
    fun toChallengeType(value: String): ChallengeType = runCatching {
        ChallengeType.valueOf(value)
    }.getOrDefault(ChallengeType.WALK)

    @TypeConverter
    fun fromStepTrackingMode(mode: StepTrackingMode): String = mode.name

    @TypeConverter
    fun toStepTrackingMode(value: String): StepTrackingMode = runCatching {
        StepTrackingMode.valueOf(value)
    }.getOrDefault(StepTrackingMode.HARDWARE_COUNTER)
}
