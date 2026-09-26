package app.wakewalk.alarm.service

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.wakewalk.alarm.audio.AudioController
import app.wakewalk.alarm.notification.AlarmNotificationManager
import app.wakewalk.alarm.scheduler.AlarmScheduler
import app.wakewalk.alarm.scheduler.AndroidAlarmScheduler
import app.wakewalk.alarm.sensor.AndroidSensorAdapter
import app.wakewalk.alarm.vibration.VibrationController
import app.wakewalk.data.local.entity.AlarmHistoryEntity
import app.wakewalk.domain.challenge.ChallengeEngine
import app.wakewalk.domain.model.AlarmStatus
import app.wakewalk.domain.model.ChallengeType
import app.wakewalk.domain.model.WakeSession
import app.wakewalk.domain.movement.MovementValidator
import app.wakewalk.domain.repository.AlarmRepository
import app.wakewalk.domain.repository.WakeSessionRepository
import app.wakewalk.ui.alarm.AlarmActivity
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
        const val ACTION_COMPLETE_CHALLENGE = "app.wakewalk.action.COMPLETE_CHALLENGE"
        const val ACTION_EMERGENCY_DISMISS = "app.wakewalk.ACTION_EMERGENCY_DISMISS"
        const val ACTION_SNOOZE = "app.wakewalk.ACTION_SNOOZE"
        const val NOTIFICATION_ID = 1001
    }

    @Inject lateinit var wakeSessionRepository: WakeSessionRepository
    @Inject lateinit var alarmRepository: AlarmRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var audioController: AudioController
    @Inject lateinit var vibrationController: VibrationController
    @Inject lateinit var sensorAdapter: AndroidSensorAdapter
    @Inject lateinit var notificationManager: AlarmNotificationManager

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val challengeEngine = ChallengeEngine(validator = MovementValidator())
    private val isTerminated = AtomicBoolean(false)

    private var cpuWakeLock: PowerManager.WakeLock? = null
    private var currentAlarmId: Long = 0L
    private var currentLabel: String = "Alarm"
    private var currentChallengeType: ChallengeType = ChallengeType.WALK
    private var currentQrPayload: String? = null
    private var currentQrLabel: String? = null
    private var targetSteps: Int = 150
    private var currentSnoozeDurationMinutes: Int = 5
    private var currentSoundUri: String? = null
    private var startedAtEpochMs: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        // Critical: Promote to Foreground within 5 seconds of creation
        promoteToForeground()

        when (action) {
            ACTION_START_ALARM -> handleStartAlarm(intent)
            ACTION_COMPLETE_CHALLENGE -> handleCompleteChallenge()
            ACTION_EMERGENCY_DISMISS -> handleEmergencyDismiss()
            ACTION_SNOOZE -> handleSnooze()
        }

        return START_NOT_STICKY
    }

    private fun promoteToForeground() {
        val notification = notificationManager.buildRingingNotification(
            alarmId = currentAlarmId,
            alarmLabel = currentLabel,
            targetSteps = targetSteps,
            currentSteps = challengeEngine.currentSession?.currentSteps ?: 0,
            challengeType = currentChallengeType,
            qrPayload = currentQrPayload,
            qrLabel = currentQrLabel
        )

        val hasActivityRecognition = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (hasActivityRecognition) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK or ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            }
        } else {
            0
        }

        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                foregroundServiceType
            )
            Log.d("WakeWalk", "AlarmForegroundService: Started foreground with type=$foregroundServiceType")
        } catch (e: Exception) {
            Log.e("WakeWalk", "Failed to startForeground with type $foregroundServiceType, falling back to 0", e)
            try {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    0
                )
            } catch (fallbackError: Exception) {
                Log.e("WakeWalk", "Fatal error starting foreground service", fallbackError)
            }
        }
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
        currentSnoozeDurationMinutes = intent.getIntExtra(AndroidAlarmScheduler.EXTRA_SNOOZE_DURATION, 5)
        val challengeTypeName = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_CHALLENGE_TYPE)
        currentChallengeType = challengeTypeName?.let {
            runCatching { ChallengeType.valueOf(it) }.getOrNull()
        } ?: ChallengeType.WALK
        val qrPayload = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_QR_PAYLOAD)
        val qrLabel = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_QR_LABEL)
        currentQrPayload = qrPayload
        currentQrLabel = qrLabel
        val vibrationEnabled = intent.getBooleanExtra(AndroidAlarmScheduler.EXTRA_VIBRATION, true)
        val gradualVolume = intent.getBooleanExtra(AndroidAlarmScheduler.EXTRA_GRADUAL_VOLUME, true)
        currentSoundUri = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_SOUND_URI)
        startedAtEpochMs = System.currentTimeMillis()

        // Acquire persistent CPU WakeLock so Doze / screen sleep cannot pause the alarm
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (cpuWakeLock == null) {
            cpuWakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "WakeWalk:AlarmForegroundServiceWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(30 * 60 * 1000L) // 30-minute safety limit
            }
        }

        // Wake screen so full-screen alarm activity is visible immediately
        try {
            @Suppress("DEPRECATION")
            val screenWakeLock = powerManager?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "WakeWalk:AlarmScreenWakeLock"
            )
            screenWakeLock?.acquire(10_000L)
        } catch (e: Exception) {
            Log.w("WakeWalk", "AlarmForegroundService: Could not acquire screen wake lock", e)
        }

        // Re-promote with updated currentAlarmId and labels
        promoteToForeground()

        // Also launch AlarmActivity explicitly so challenge UI appears immediately
        try {
            val activityIntent = Intent(this, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(AndroidAlarmScheduler.EXTRA_ALARM_ID, currentAlarmId)
                putExtra(AndroidAlarmScheduler.EXTRA_TARGET_STEPS, targetSteps)
                putExtra(AndroidAlarmScheduler.EXTRA_ALARM_LABEL, currentLabel)
                putExtra(AndroidAlarmScheduler.EXTRA_CHALLENGE_TYPE, currentChallengeType.name)
                putExtra(AndroidAlarmScheduler.EXTRA_QR_PAYLOAD, qrPayload)
                putExtra(AndroidAlarmScheduler.EXTRA_QR_LABEL, qrLabel)
            }
            startActivity(activityIntent)
            Log.d("WakeWalk", "AlarmForegroundService: Launched AlarmActivity directly")
        } catch (e: Exception) {
            Log.e("WakeWalk", "AlarmForegroundService: Failed to launch AlarmActivity directly", e)
        }

        serviceScope.launch {
            // Update alarm schedule: repeating alarms rescheduled for next occurrence; one-time alarms disabled in DB
            try {
                if (currentAlarmId > 0) {
                    val alarm = alarmRepository.getAlarmById(currentAlarmId)
                    if (alarm != null) {
                        if (alarm.repeatDaysMask != 0) {
                            // Repeating alarm: reschedule for next occurrence
                            alarmScheduler.scheduleAlarm(alarm, afterEpochMs = startedAtEpochMs)
                            Log.d("WakeWalk", "AlarmForegroundService: Repeating alarm $currentAlarmId rescheduled for next occurrence")
                        } else {
                            // One-time alarm: mark disabled in DB so UI toggle accurately reflects that it completed
                            alarmRepository.updateAlarm(alarm.copy(isEnabled = false, updatedAtEpochMs = System.currentTimeMillis()))
                            Log.d("WakeWalk", "AlarmForegroundService: One-time alarm $currentAlarmId marked disabled in DB")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("WakeWalk", "AlarmForegroundService: Error updating alarm schedule", e)
            }
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
            audioController.startAlarmAudio(customSoundUri = currentSoundUri, gradualVolume = gradualVolume)
            if (vibrationEnabled) {
                vibrationController.startAlarmVibration()
            }

            // Start Sensor Adapter
            sensorAdapter.start { stepInput, motionSnapshot ->
                val prevSteps = challengeEngine.currentSession?.currentSteps ?: 0
                val result = challengeEngine.processStep(stepInput, motionSnapshot)
                val newSteps = challengeEngine.currentSession?.currentSteps ?: 0
                Log.d(
                    "WakeWalk",
                    "AlarmForegroundService: processStep accepted=${result.accepted}, reason=${result.reason}, steps=$newSteps/$targetSteps"
                )
                if (result.accepted && newSteps > prevSteps) {
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
                            if (currentChallengeType == ChallengeType.WALK) {
                                Log.d("WakeWalk", "AlarmForegroundService: Target steps reached! Silencing alarm.")
                                completeSession(AlarmStatus.CHALLENGE_COMPLETED)
                            } else {
                                Log.d("WakeWalk", "AlarmForegroundService: Walk-to-target steps reached for QR challenge. Awaiting physical QR code scan.")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateProgressNotification(currentSteps: Int) {
        val notification = notificationManager.buildRingingNotification(
            alarmId = currentAlarmId,
            alarmLabel = currentLabel,
            targetSteps = targetSteps,
            currentSteps = currentSteps,
            challengeType = currentChallengeType,
            qrPayload = currentQrPayload,
            qrLabel = currentQrLabel
        )
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun handleCompleteChallenge() {
        Log.d("WakeWalk", "AlarmForegroundService: handleCompleteChallenge() called via ACTION_COMPLETE_CHALLENGE")
        challengeEngine.currentSession?.let { session ->
            challengeEngine.restoreSession(
                session.copy(status = AlarmStatus.CHALLENGE_COMPLETED)
            )
        }
        completeSession(AlarmStatus.CHALLENGE_COMPLETED)
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
        if (currentAlarmId > 0) {
            alarmScheduler.scheduleSnooze(currentAlarmId, currentSnoozeDurationMinutes)
            Log.d("WakeWalk", "AlarmForegroundService: Scheduled snooze for alarm $currentAlarmId for $currentSnoozeDurationMinutes min")
        }
        completeSession(AlarmStatus.SNOOZED)
    }

    private fun completeSession(status: AlarmStatus) {
        if (!isTerminated.compareAndSet(false, true)) {
            return // Prevent duplicate teardown from racing callbacks
        }

        // Release CPU wake lock
        try {
            if (cpuWakeLock?.isHeld == true) {
                cpuWakeLock?.release()
            }
        } catch (_: Exception) {}
        cpuWakeLock = null

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
        try {
            if (cpuWakeLock?.isHeld == true) {
                cpuWakeLock?.release()
            }
        } catch (_: Exception) {}
        cpuWakeLock = null
        sensorAdapter.stop()
        audioController.stopAudio()
        vibrationController.stopVibration()
        serviceScope.cancel()
    }
}
