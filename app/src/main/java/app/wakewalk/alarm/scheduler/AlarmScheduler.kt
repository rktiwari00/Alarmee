package app.wakewalk.alarm.scheduler

import app.wakewalk.data.local.entity.AlarmEntity

interface AlarmScheduler {
    fun scheduleAlarm(alarm: AlarmEntity, afterEpochMs: Long? = null)
    fun scheduleSnooze(alarmId: Long, snoozeDurationMinutes: Int)
    fun scheduleTestAlarm(delaySeconds: Int = 5)
    fun cancelAlarm(alarmId: Long)
    suspend fun reconcileAlarms()
    fun canScheduleExactAlarms(): Boolean
}
