package app.wakewalk.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.wakewalk.alarm.scheduler.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Reconcile AlarmManager with enabled database alarms
                // Note: We deliberately do NOT start the Foreground Service from boot!
                alarmScheduler.reconcileAlarms()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
