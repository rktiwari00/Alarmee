package app.wakewalk.data.repository

import app.wakewalk.data.local.dao.AlarmDao
import app.wakewalk.data.local.entity.AlarmEntity
import app.wakewalk.domain.repository.AlarmRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepositoryImpl @Inject constructor(
    private val alarmDao: AlarmDao
) : AlarmRepository {

    override fun getAllAlarmsFlow(): Flow<List<AlarmEntity>> = alarmDao.getAllAlarmsFlow()

    override suspend fun getEnabledAlarms(): List<AlarmEntity> = alarmDao.getEnabledAlarms()

    override suspend fun getAlarmById(id: Long): AlarmEntity? = alarmDao.getAlarmById(id)

    override suspend fun insertAlarm(alarm: AlarmEntity): Long = alarmDao.insertAlarm(alarm)

    override suspend fun updateAlarm(alarm: AlarmEntity) = alarmDao.updateAlarm(alarm)

    override suspend fun deleteAlarm(alarm: AlarmEntity) = alarmDao.deleteAlarm(alarm)

    override suspend fun toggleAlarm(alarm: AlarmEntity): Boolean {
        val newEnabled = !alarm.isEnabled
        alarmDao.updateAlarm(alarm.copy(isEnabled = newEnabled, updatedAtEpochMs = System.currentTimeMillis()))
        return newEnabled
    }
}
