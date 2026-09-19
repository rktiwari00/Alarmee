package app.wakewalk.domain.repository

import app.wakewalk.data.local.entity.AlarmEntity
import kotlinx.coroutines.flow.Flow

interface AlarmRepository {
    fun getAllAlarmsFlow(): Flow<List<AlarmEntity>>
    suspend fun getEnabledAlarms(): List<AlarmEntity>
    suspend fun getAlarmById(id: Long): AlarmEntity?
    suspend fun insertAlarm(alarm: AlarmEntity): Long
    suspend fun updateAlarm(alarm: AlarmEntity)
    suspend fun deleteAlarm(alarm: AlarmEntity)
    suspend fun toggleAlarm(alarm: AlarmEntity): Boolean
}
