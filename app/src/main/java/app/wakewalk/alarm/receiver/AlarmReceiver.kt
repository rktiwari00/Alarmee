package app.wakewalk.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import app.wakewalk.alarm.scheduler.AndroidAlarmScheduler
import app.wakewalk.alarm.service.AlarmForegroundService

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val alarmId = intent.getLongExtra(AndroidAlarmScheduler.EXTRA_ALARM_ID, -1L)
        Log.d("WakeWalk", "AlarmReceiver: Received broadcast action=$action, alarmId=$alarmId")

        if (action != AndroidAlarmScheduler.ACTION_TRIGGER_ALARM) return

        // Acquire short-lived wake lock (15s safety timeout) to bridge until service promotion.
        // DO NOT release immediately in finally because startForegroundService is asynchronous.
        // The 15-second timeout guarantees automatic release by the OS.
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WakeWalk:AlarmReceiverWakeLock"
        )?.apply {
            setReferenceCounted(false)
        }
        wakeLock?.acquire(15_000L)

        val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
            this.action = AlarmForegroundService.ACTION_START_ALARM
            putExtras(intent)
        }

        try {
            ContextCompat.startForegroundService(context, serviceIntent)
            Log.d("WakeWalk", "AlarmReceiver: Successfully dispatched startForegroundService")
        } catch (e: Exception) {
            Log.e("WakeWalk", "AlarmReceiver: Failed to start foreground service", e)
            if (wakeLock != null && wakeLock.isHeld) {
                try {
                    wakeLock.release()
                } catch (_: Exception) {
                    // Lock may have expired
                }
            }
        }
    }
}
