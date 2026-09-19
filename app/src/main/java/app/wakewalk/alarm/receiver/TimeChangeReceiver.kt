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
class TimeChangeReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_TIME_CHANGED && action != Intent.ACTION_TIMEZONE_CHANGED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                alarmScheduler.reconcileAlarms()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
