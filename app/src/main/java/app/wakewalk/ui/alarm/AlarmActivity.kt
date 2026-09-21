package app.wakewalk.ui.alarm

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import app.wakewalk.alarm.scheduler.AndroidAlarmScheduler
import app.wakewalk.domain.model.ChallengeType
import app.wakewalk.ui.theme.WakeWalkTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    private val viewModel: AlarmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        // Lock to portrait orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Show over lockscreen and turn screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        // Keep screen on while ringing/challenging
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Intercept and disable back button so user cannot accidentally or intentionally dismiss alarm
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing: user must walk or use the emergency bypass
            }
        })

        setContent {
            WakeWalkTheme(darkTheme = true) {
                AlarmScreen(
                    viewModel = viewModel,
                    onDismissActivity = {
                        finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val challengeTypeStr = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_CHALLENGE_TYPE)
        val challengeType = try {
            if (challengeTypeStr != null) ChallengeType.valueOf(challengeTypeStr) else ChallengeType.WALK
        } catch (e: Exception) {
            ChallengeType.WALK
        }
        val targetSteps = intent.getIntExtra(AndroidAlarmScheduler.EXTRA_TARGET_STEPS, 150)
        val qrPayload = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_QR_PAYLOAD)
        val qrLabel = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_QR_LABEL)

        viewModel.initChallengeData(
            type = challengeType,
            targetSteps = targetSteps,
            qrPayload = qrPayload,
            qrLabel = qrLabel
        )
    }
}
