package app.wakewalk.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat
import app.wakewalk.alarm.scheduler.AndroidAlarmScheduler
import app.wakewalk.alarm.service.AlarmForegroundService

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AndroidAlarmScheduler.ACTION_TRIGGER_ALARM) return

        // Acquire short-lived wake lock (max 10s safety timeout) to bridge until service promotion
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WakeWalk:AlarmReceiverWakeLock"
        )
        wakeLock?.acquire(10_000L)

        val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
            action = AlarmForegroundService.ACTION_START_ALARM
            putExtras(intent)
        }

        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } finally {
            // If service start fails or is queued, release lock safely
            if (wakeLock != null && wakeLock.isHeld) {
                try {
                    wakeLock.release()
                } catch (_: RuntimeException) {
                    // Lock may already have been released or expired
                }
            }
        }
    }
}
