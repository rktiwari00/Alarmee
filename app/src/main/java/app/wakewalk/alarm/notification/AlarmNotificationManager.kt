package app.wakewalk.alarm.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import app.wakewalk.alarm.scheduler.AndroidAlarmScheduler
import app.wakewalk.domain.model.ChallengeType
import app.wakewalk.ui.alarm.AlarmActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "wakewalk_alarm_channel_high"
        const val CHANNEL_NAME = "WakeWalk Alarms"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority alarms for WakeWalk"
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 300, 500)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun canUseFullScreenIntent(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            notificationManager.canUseFullScreenIntent()
        } else {
            true
        }
    }

    fun buildRingingNotification(
        alarmLabel: String,
        targetSteps: Int,
        currentSteps: Int,
        challengeType: ChallengeType = ChallengeType.WALK,
        qrPayload: String? = null,
        qrLabel: String? = null
    ): Notification {
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AndroidAlarmScheduler.EXTRA_TARGET_STEPS, targetSteps)
            putExtra(AndroidAlarmScheduler.EXTRA_ALARM_LABEL, alarmLabel)
            putExtra(AndroidAlarmScheduler.EXTRA_CHALLENGE_TYPE, challengeType.name)
            putExtra(AndroidAlarmScheduler.EXTRA_QR_PAYLOAD, qrPayload)
            putExtra(AndroidAlarmScheduler.EXTRA_QR_LABEL, qrLabel)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (challengeType == ChallengeType.QR_CODE) {
            if (currentSteps < targetSteps) {
                "Walk to target: $currentSteps / $targetSteps steps"
            } else {
                "Steps done! Scan ${qrLabel ?: "registered code"} to dismiss"
            }
        } else {
            "Walk to stop alarm: $currentSteps / $targetSteps steps"
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("WAKE UP — $alarmLabel")
            .setContentText(contentText)
            .setProgress(targetSteps, currentSteps, false)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_directions,
                "Open Challenge",
                fullScreenPendingIntent
            )
            .build()
    }

    fun buildCompletionNotification(stepsWalked: Int, durationSeconds: Long): Notification {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val durationStr = "${durationSeconds / 60}m ${durationSeconds % 60}s"

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("✓ WakeWalk Complete!")
            .setContentText("You walked $stepsWalked steps in $durationStr. Good morning! ☀️")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
    }
}
