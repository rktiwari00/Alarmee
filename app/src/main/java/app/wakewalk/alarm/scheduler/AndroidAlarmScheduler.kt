package app.wakewalk.alarm.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
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
        const val EXTRA_CHALLENGE_TYPE = "extra_challenge_type"
        const val EXTRA_QR_PAYLOAD = "extra_qr_payload"
        const val EXTRA_QR_LABEL = "extra_qr_label"
        const val EXTRA_VIBRATION = "extra_vibration"
        const val EXTRA_GRADUAL_VOLUME = "extra_gradual_volume"
        const val EXTRA_SNOOZE_ENABLED = "extra_snooze_enabled"
        const val EXTRA_SNOOZE_DURATION = "extra_snooze_duration"
        const val EXTRA_SOUND_URI = "extra_sound_uri"
        const val EXTRA_LOWER_VOLUME_WHILE_WALKING = "extra_lower_volume_while_walking"
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

    override fun scheduleAlarm(alarm: AlarmEntity, afterEpochMs: Long?) {
        if (!alarm.isEnabled) {
            cancelAlarm(alarm.id)
            return
        }

        val triggerTimeEpochMs = if (afterEpochMs != null) {
            AlarmTimeCalculator.calculateNextOccurrenceAfter(
                hour = alarm.hour,
                minute = alarm.minute,
                repeatDaysMask = alarm.repeatDaysMask,
                afterEpochMs = afterEpochMs
            )
        } else {
            AlarmTimeCalculator.calculateNextTriggerTime(
                hour = alarm.hour,
                minute = alarm.minute,
                repeatDaysMask = alarm.repeatDaysMask
            )
        }

        val remainingSec = (triggerTimeEpochMs - System.currentTimeMillis()) / 1000
        Log.d(
            "WakeWalk",
            "AndroidAlarmScheduler: Scheduling alarm ${alarm.id} (${alarm.hour}:${alarm.minute}) to trigger at $triggerTimeEpochMs (in ${remainingSec / 60}m ${remainingSec % 60}s)"
        )

        val triggerIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_TARGET_STEPS, alarm.targetSteps)
            putExtra(EXTRA_ALARM_LABEL, alarm.label)
            putExtra(EXTRA_CHALLENGE_TYPE, alarm.challengeType.name)
            putExtra(EXTRA_QR_PAYLOAD, alarm.qrCodePayload)
            putExtra(EXTRA_QR_LABEL, alarm.qrCodeLabel)
            putExtra(EXTRA_VIBRATION, alarm.vibrationEnabled)
            putExtra(EXTRA_GRADUAL_VOLUME, alarm.gradualVolume)
            putExtra(EXTRA_LOWER_VOLUME_WHILE_WALKING, alarm.lowerVolumeWhileWalking)
            putExtra(EXTRA_SNOOZE_ENABLED, alarm.snoozeEnabled)
            putExtra(EXTRA_SNOOZE_DURATION, alarm.snoozeDurationMinutes)
            putExtra(EXTRA_SOUND_URI, alarm.soundUri)
        }

        val pendingTriggerIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            triggerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Show Intent for system UI / Lockscreen Clock
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
            Log.d("WakeWalk", "AndroidAlarmScheduler: setAlarmClock succeeded for alarm ${alarm.id}")
        } catch (e: SecurityException) {
            Log.w("WakeWalk", "AndroidAlarmScheduler: setAlarmClock failed with SecurityException, trying setExactAndAllowWhileIdle", e)
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeEpochMs,
                    pendingTriggerIntent
                )
                Log.d("WakeWalk", "AndroidAlarmScheduler: setExactAndAllowWhileIdle succeeded for alarm ${alarm.id}")
            } catch (exactError: SecurityException) {
                Log.w("WakeWalk", "AndroidAlarmScheduler: setExactAndAllowWhileIdle failed, falling back to setAndAllowWhileIdle", exactError)
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeEpochMs,
                    pendingTriggerIntent
                )
            }
        }
    }

    override fun scheduleTestAlarm(delaySeconds: Int) {
        val triggerTimeEpochMs = System.currentTimeMillis() + (delaySeconds * 1000L)
        Log.d("WakeWalk", "AndroidAlarmScheduler: Scheduling TEST alarm for $delaySeconds seconds from now")

        val triggerIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            putExtra(EXTRA_ALARM_ID, 999999L)
            putExtra(EXTRA_TARGET_STEPS, 10)
            putExtra(EXTRA_ALARM_LABEL, "Test Alarm")
            putExtra(EXTRA_VIBRATION, true)
            putExtra(EXTRA_GRADUAL_VOLUME, false)
            putExtra(EXTRA_SNOOZE_ENABLED, false)
            putExtra(EXTRA_SNOOZE_DURATION, 1)
        }

        val pendingTriggerIntent = PendingIntent.getBroadcast(
            context,
            999999,
            triggerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()
        val pendingShowIntent = PendingIntent.getActivity(
            context,
            999999,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeEpochMs, pendingShowIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingTriggerIntent)
            Log.d("WakeWalk", "AndroidAlarmScheduler: Test alarm scheduled via setAlarmClock")
        } catch (e: SecurityException) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeEpochMs,
                    pendingTriggerIntent
                )
                Log.d("WakeWalk", "AndroidAlarmScheduler: Test alarm scheduled via setExactAndAllowWhileIdle")
            } catch (_: SecurityException) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeEpochMs,
                    pendingTriggerIntent
                )
                Log.d("WakeWalk", "AndroidAlarmScheduler: Test alarm scheduled via setAndAllowWhileIdle")
            }
        }
    }

    override fun scheduleSnooze(alarmId: Long, snoozeDurationMinutes: Int) {
        val triggerTimeEpochMs = System.currentTimeMillis() + (snoozeDurationMinutes * 60_000L)
        val remainingSec = (triggerTimeEpochMs - System.currentTimeMillis()) / 1000
        Log.d(
            "WakeWalk",
            "AndroidAlarmScheduler: Scheduling snooze for alarm $alarmId in $snoozeDurationMinutes min (at $triggerTimeEpochMs)"
        )

        val triggerIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_LABEL, "Snoozed Alarm")
            putExtra(EXTRA_VIBRATION, true)
            putExtra(EXTRA_GRADUAL_VOLUME, true)
            putExtra(EXTRA_SNOOZE_ENABLED, true)
            putExtra(EXTRA_SNOOZE_DURATION, snoozeDurationMinutes)
        }

        val snoozeRequestCode = (alarmId + 100_000L).toInt()
        val pendingTriggerIntent = PendingIntent.getBroadcast(
            context,
            snoozeRequestCode,
            triggerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()
        val pendingShowIntent = PendingIntent.getActivity(
            context,
            snoozeRequestCode,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeEpochMs, pendingShowIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingTriggerIntent)
            Log.d("WakeWalk", "AndroidAlarmScheduler: Snooze scheduled via setAlarmClock for alarm $alarmId")
        } catch (e: SecurityException) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeEpochMs,
                    pendingTriggerIntent
                )
            } catch (_: SecurityException) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeEpochMs,
                    pendingTriggerIntent
                )
            }
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
            Log.d("WakeWalk", "AndroidAlarmScheduler: Canceled alarm $alarmId")
        }

        val snoozeRequestCode = (alarmId + 100_000L).toInt()
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            snoozeRequestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (snoozePendingIntent != null) {
            alarmManager.cancel(snoozePendingIntent)
            snoozePendingIntent.cancel()
            Log.d("WakeWalk", "AndroidAlarmScheduler: Canceled snooze for alarm $alarmId")
        }
    }

    override suspend fun reconcileAlarms() {
        val enabledAlarms = alarmRepository.getEnabledAlarms()
        for (alarm in enabledAlarms) {
            scheduleAlarm(alarm)
        }
    }
}
