package app.wakewalk.alarm.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import app.wakewalk.alarm.audio.AudioController
import app.wakewalk.alarm.notification.AlarmNotificationManager
import app.wakewalk.alarm.scheduler.AndroidAlarmScheduler
import app.wakewalk.alarm.sensor.AndroidSensorAdapter
import app.wakewalk.alarm.vibration.VibrationController
import app.wakewalk.data.local.entity.AlarmHistoryEntity
import app.wakewalk.domain.challenge.ChallengeEngine
import app.wakewalk.domain.model.AlarmStatus
import app.wakewalk.domain.model.WakeSession
import app.wakewalk.domain.movement.MovementValidator
import app.wakewalk.domain.repository.WakeSessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class AlarmForegroundService : Service() {

    companion object {
        const val ACTION_START_ALARM = "app.wakewalk.ACTION_START_ALARM"
        const val ACTION_EMERGENCY_DISMISS = "app.wakewalk.ACTION_EMERGENCY_DISMISS"
        const val ACTION_SNOOZE = "app.wakewalk.ACTION_SNOOZE"
        const val NOTIFICATION_ID = 1001
    }

    @Inject lateinit var wakeSessionRepository: WakeSessionRepository
    @Inject lateinit var audioController: AudioController
    @Inject lateinit var vibrationController: VibrationController
    @Inject lateinit var sensorAdapter: AndroidSensorAdapter
    @Inject lateinit var notificationManager: AlarmNotificationManager

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val challengeEngine = ChallengeEngine(validator = MovementValidator())
    private val isTerminated = AtomicBoolean(false)

    private var currentAlarmId: Long = 0L
    private var currentLabel: String = "Alarm"
    private var targetSteps: Int = 150
    private var startedAtEpochMs: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        // Critical: Promote to Foreground within 5 seconds of creation
        promoteToForeground()

        when (action) {
            ACTION_START_ALARM -> handleStartAlarm(intent)
            ACTION_EMERGENCY_DISMISS -> handleEmergencyDismiss()
            ACTION_SNOOZE -> handleSnooze()
        }

        return START_NOT_STICKY
    }

    private fun promoteToForeground() {
        val notification = notificationManager.buildRingingNotification(
            alarmLabel = currentLabel,
            targetSteps = targetSteps,
            currentSteps = challengeEngine.currentSession?.currentSteps ?: 0
        )

        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        } else {
            0
        }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            foregroundServiceType
        )
    }

    private fun handleStartAlarm(intent: Intent) {
        // Enforce One-Active-Session policy: If already active, do not overwrite or start second challenge
        val existingSession = challengeEngine.currentSession
        if (existingSession != null && existingSession.status == AlarmStatus.CHALLENGE_ACTIVE) {
            return
        }

        currentAlarmId = intent.getLongExtra(AndroidAlarmScheduler.EXTRA_ALARM_ID, 0L)
        currentLabel = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_ALARM_LABEL) ?: "Wake Up"
        targetSteps = intent.getIntExtra(AndroidAlarmScheduler.EXTRA_TARGET_STEPS, 150)
        val vibrationEnabled = intent.getBooleanExtra(AndroidAlarmScheduler.EXTRA_VIBRATION, true)
        val gradualVolume = intent.getBooleanExtra(AndroidAlarmScheduler.EXTRA_GRADUAL_VOLUME, true)
        startedAtEpochMs = System.currentTimeMillis()

        serviceScope.launch {
            // Check if recoverable session exists in Room
            val restored = wakeSessionRepository.restoreSessionFromDb()
            if (restored != null && restored.alarmId == currentAlarmId && !restored.isComplete) {
                challengeEngine.restoreSession(restored)
            } else {
                val newSession = WakeSession(
                    sessionId = UUID.randomUUID().toString(),
                    alarmId = currentAlarmId,
                    targetSteps = targetSteps,
                    initialSensorSteps = 0L,
                    currentSteps = 0,
                    status = AlarmStatus.CHALLENGE_ACTIVE,
                    startedAtEpochMs = startedAtEpochMs,
                    startedRealtimeNs = System.nanoTime(),
                    trackingMode = sensorAdapter.resolvedTrackingMode
                )
                challengeEngine.startSession(
                    sessionId = newSession.sessionId,
                    alarmId = currentAlarmId,
                    targetSteps = targetSteps,
                    initialSensorSteps = 0L,
                    trackingMode = sensorAdapter.resolvedTrackingMode,
                    startedAtEpochMs = startedAtEpochMs,
                    startedRealtimeNs = newSession.startedRealtimeNs
                )
                wakeSessionRepository.startSession(newSession)
            }

            // Start Audio and Vibration
            audioController.startAlarmAudio(gradualVolume = gradualVolume)
            if (vibrationEnabled) {
                vibrationController.startAlarmVibration()
            }

            // Start Sensor Adapter
            sensorAdapter.start { stepInput, motionSnapshot ->
                val result = challengeEngine.processStep(stepInput, motionSnapshot)
                if (result.accepted) {
                    val updatedSession = challengeEngine.currentSession ?: return@start
                    serviceScope.launch {
                        wakeSessionRepository.updateSession(updatedSession)
                        updateProgressNotification(updatedSession.currentSteps)

                        // Milestone haptics
                        val fraction = updatedSession.progress.fraction
                        if (fraction in 0.24f..0.26f || fraction in 0.49f..0.51f || fraction in 0.74f..0.76f) {
                            vibrationController.triggerMilestoneHaptic()
                        }

                        if (updatedSession.isComplete) {
                            completeSession(AlarmStatus.CHALLENGE_COMPLETED)
                        }
                    }
                }
            }
        }
    }

    private fun updateProgressNotification(currentSteps: Int) {
        val notification = notificationManager.buildRingingNotification(
            alarmLabel = currentLabel,
            targetSteps = targetSteps,
            currentSteps = currentSteps
        )
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun handleEmergencyDismiss() {
        challengeEngine.attemptEmergencyDismissal(
            app.wakewalk.domain.emergency.EmergencyDismissalAttempt.PhraseConfirmed("I AM AWAKE"),
            System.currentTimeMillis()
        )
        completeSession(AlarmStatus.EMERGENCY_DISMISSED)
    }

    private fun handleSnooze() {
        challengeEngine.snooze(System.currentTimeMillis())
        completeSession(AlarmStatus.SNOOZED)
    }

    private fun completeSession(status: AlarmStatus) {
        if (!isTerminated.compareAndSet(false, true)) {
            return // Prevent duplicate teardown from racing callbacks
        }

        val session = challengeEngine.currentSession
        val completedSteps = session?.currentSteps ?: 0
        val sessionId = session?.sessionId ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val durationSeconds = if (startedAtEpochMs > 0L) (now - startedAtEpochMs) / 1000L else 0L

        // 1. Teardown audio and sensors immediately
        sensorAdapter.stop()
        audioController.stopAudio()
        vibrationController.stopVibration()

        if (status == AlarmStatus.CHALLENGE_COMPLETED) {
            vibrationController.triggerSuccessHaptic()
        }

        // 2. Persist history and clear active session atomically in Room
        serviceScope.launch {
            val history = AlarmHistoryEntity(
                sessionId = sessionId,
                alarmId = currentAlarmId,
                scheduledTimeEpochMs = startedAtEpochMs,
                triggeredAtEpochMs = startedAtEpochMs,
                completedAtEpochMs = now,
                status = status,
                targetSteps = targetSteps,
                completedSteps = completedSteps,
                durationSeconds = durationSeconds,
                emergencyDismissed = status == AlarmStatus.EMERGENCY_DISMISSED
            )
            wakeSessionRepository.completeSession(history)

            // 3. Post completion notification
            if (status == AlarmStatus.CHALLENGE_COMPLETED) {
                val completionNotification = notificationManager.buildCompletionNotification(
                    stepsWalked = completedSteps,
                    durationSeconds = durationSeconds
                )
                val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                nm.notify(NOTIFICATION_ID + 1, completionNotification)
            }

            // 4. Stop Foreground & Stop Service
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorAdapter.stop()
        audioController.stopAudio()
        vibrationController.stopVibration()
        serviceScope.cancel()
    }
}
