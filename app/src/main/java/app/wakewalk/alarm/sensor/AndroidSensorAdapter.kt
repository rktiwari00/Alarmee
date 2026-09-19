package app.wakewalk.alarm.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import app.wakewalk.domain.model.MotionSnapshot
import app.wakewalk.domain.model.StepInput
import app.wakewalk.domain.model.StepTrackingMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class AndroidSensorAdapter @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val stepCounterSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetectorSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val accelerometerSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val resolvedTrackingMode: StepTrackingMode = when {
        stepCounterSensor != null -> StepTrackingMode.HARDWARE_COUNTER
        stepDetectorSensor != null -> StepTrackingMode.HARDWARE_DETECTOR
        accelerometerSensor != null -> StepTrackingMode.ACCELEROMETER_FALLBACK
        else -> StepTrackingMode.UNAVAILABLE
    }

    private var isListening = false
    private var callback: ((StepInput, MotionSnapshot?) -> Unit)? = null
    private var latestMotion: MotionSnapshot? = null

    // Fallback peak detector variables
    private var lastPeakTimeNs = 0L
    private var lastMagnitude = 9.8f
    private var isRising = false

    fun start(onStepInput: (StepInput, MotionSnapshot?) -> Unit) {
        if (isListening) return
        isListening = true
        callback = onStepInput

        val sm = sensorManager ?: return

        // Always register accelerometer for anti-cheat motion snapshot
        accelerometerSensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }

        // Register primary step sensor
        when (resolvedTrackingMode) {
            StepTrackingMode.HARDWARE_COUNTER -> {
                stepCounterSensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            }
            StepTrackingMode.HARDWARE_DETECTOR -> {
                stepDetectorSensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            }
            StepTrackingMode.ACCELEROMETER_FALLBACK -> {
                // Handled via accelerometer listener
            }
            StepTrackingMode.UNAVAILABLE -> {
                // No sensors available
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val nowNs = event.timestamp
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val motion = MotionSnapshot(timestampNs = nowNs, x = x, y = y, z = z)
                latestMotion = motion

                if (resolvedTrackingMode == StepTrackingMode.ACCELEROMETER_FALLBACK) {
                    processAccelerometerFallback(motion)
                }
            }
            Sensor.TYPE_STEP_COUNTER -> {
                val cumulative = event.values[0].toLong()
                val input = StepInput.CounterUpdate(timestampNs = nowNs, cumulativeSteps = cumulative)
                callback?.invoke(input, latestMotion)
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                val input = StepInput.DetectorStep(timestampNs = nowNs)
                callback?.invoke(input, latestMotion)
            }
        }
    }

    private fun processAccelerometerFallback(motion: MotionSnapshot) {
        val magnitude = motion.magnitude
        val delta = magnitude - lastMagnitude

        // Peak detection with threshold (11.5 m/s² ~ 1.17G threshold for walking step)
        if (delta > 0.5f && !isRising) {
            isRising = true
        } else if (delta < -0.5f && isRising) {
            isRising = false
            if (magnitude > 11.2f) {
                val intervalNs = motion.timestampNs - lastPeakTimeNs
                if (intervalNs > 320_000_000L) { // > 320ms interval
                    lastPeakTimeNs = motion.timestampNs
                    val input = StepInput.AccelerometerCandidate(motion.timestampNs, motion)
                    callback?.invoke(input, motion)
                }
            }
        }
        lastMagnitude = magnitude
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No action needed
    }

    fun stop() {
        if (!isListening) return
        isListening = false
        callback = null
        try {
            sensorManager?.unregisterListener(this)
        } catch (_: Exception) {
            // Safe cleanup
        }
    }
}
