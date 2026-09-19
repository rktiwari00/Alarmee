package app.wakewalk.alarm.vibration

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VibrationController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun startAlarmVibration() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        // Waveform: 0ms wait, 500ms on, 300ms off, 500ms on, 500ms off
        val timings = longArrayOf(0, 500, 300, 500, 500)
        val amplitudes = intArrayOf(0, 255, 0, 255, 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(timings, amplitudes, 0) // 0 = repeat from index 0
            v.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(timings, 0)
        }
    }

    fun triggerMilestoneHaptic() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(100L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(100L)
        }
    }

    fun triggerSuccessHaptic() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        // Triumph double-pulse: 150ms on, 100ms off, 250ms on
        val timings = longArrayOf(0, 150, 100, 250)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(timings, -1))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(timings, -1)
        }
    }

    fun stopVibration() {
        try {
            vibrator?.cancel()
        } catch (_: Exception) {
            // Ignored on cleanup
        }
    }
}
