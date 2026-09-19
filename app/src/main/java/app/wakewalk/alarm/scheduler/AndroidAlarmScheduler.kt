package app.wakewalk.alarm.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import app.wakewalk.alarm.receiver.AlarmReceiver
import app.wakewalk.data.local.entity.AlarmEntity
import app.wakewalk.domain.repository.AlarmRepository
import app.wakewalk.domain.scheduler.AlarmTimeCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
    private val alarmRepository: AlarmRepository
) : AlarmScheduler {

    companion object {
        const val ACTION_TRIGGER_ALARM = "app.wakewalk.ACTION_TRIGGER_ALARM"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_TARGET_STEPS = "extra_target_steps"
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"
        const val EXTRA_VIBRATION = "extra_vibration"
        const val EXTRA_GRADUAL_VOLUME = "extra_gradual_volume"
        const val EXTRA_SNOOZE_ENABLED = "extra_snooze_enabled"
        const val EXTRA_SNOOZE_DURATION = "extra_snooze_duration"
    }

    override fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                alarmManager.canScheduleExactAlarms()
            } catch (_: SecurityException) {
                false
            }
        } else {
            true
        }
    }

    override fun scheduleAlarm(alarm: AlarmEntity) {
        if (!alarm.isEnabled) {
            cancelAlarm(alarm.id)
            return
        }

        val triggerTimeEpochMs = AlarmTimeCalculator.calculateNextTriggerTime(
            hour = alarm.hour,
            minute = alarm.minute,
            repeatDaysMask = alarm.repeatDaysMask
        )

        val triggerIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_TARGET_STEPS, alarm.targetSteps)
            putExtra(EXTRA_ALARM_LABEL, alarm.label)
            putExtra(EXTRA_VIBRATION, alarm.vibrationEnabled)
            putExtra(EXTRA_GRADUAL_VOLUME, alarm.gradualVolume)
            putExtra(EXTRA_SNOOZE_ENABLED, alarm.snoozeEnabled)
            putExtra(EXTRA_SNOOZE_DURATION, alarm.snoozeDurationMinutes)
        }

        val pendingTriggerIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            triggerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Show Intent for system UI
        val showIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()
        val pendingShowIntent = PendingIntent.getActivity(
            context,
            alarm.id.toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeEpochMs, pendingShowIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingTriggerIntent)
        } catch (_: SecurityException) {
            // Graceful fallback if exact alarm permission is missing
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeEpochMs,
                pendingTriggerIntent
            )
        }
    }

    override fun cancelAlarm(alarmId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    override suspend fun reconcileAlarms() {
        val enabledAlarms = alarmRepository.getEnabledAlarms()
        for (alarm in enabledAlarms) {
            scheduleAlarm(alarm)
        }
    }
}
