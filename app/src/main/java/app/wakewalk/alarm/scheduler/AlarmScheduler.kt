package app.wakewalk.alarm.scheduler

import app.wakewalk.data.local.entity.AlarmEntity

interface AlarmScheduler {
    fun scheduleAlarm(alarm: AlarmEntity)
    fun cancelAlarm(alarmId: Long)
    suspend fun reconcileAlarms()
    fun canScheduleExactAlarms(): Boolean
}
