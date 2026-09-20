package app.wakewalk.alarm.sensor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import app.wakewalk.domain.model.MotionSnapshot
import app.wakewalk.domain.model.StepInput
import app.wakewalk.domain.model.StepTrackingMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidSensorAdapter @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val stepDetectorSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val stepCounterSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val accelerometerSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    fun hasActivityRecognitionPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    val resolvedTrackingMode: StepTrackingMode
        get() {
            val hasPermission = hasActivityRecognitionPermission()
            return when {
                hasPermission && stepDetectorSensor != null -> StepTrackingMode.HARDWARE_DETECTOR
                hasPermission && stepCounterSensor != null -> StepTrackingMode.HARDWARE_COUNTER
                accelerometerSensor != null -> StepTrackingMode.ACCELEROMETER_FALLBACK
                else -> StepTrackingMode.UNAVAILABLE
            }
        }

    private var isListening = false
    private var callback: ((StepInput, MotionSnapshot?) -> Unit)? = null
    private var latestMotion: MotionSnapshot? = null

    // Tracking mode chosen for the active session
    private var activeTrackingMode: StepTrackingMode = StepTrackingMode.UNAVAILABLE

    // Timestamp (in ms) when the last step was dispatched to avoid duplicate counting
    private var lastDispatchedStepTimeMs = 0L

    // Dynamic Gravity & AC-Centered Accelerometer Step Detector
    private var gravityBaseline = 9.8f
    private var filteredMagnitude = 9.8f
    private var lastAccStepTimeNs = 0L
    private var waveState = AccWaveState.WAITING_FOR_CREST

    private enum class AccWaveState {
        WAITING_FOR_CREST,
        WAITING_FOR_TROUGH
    }

    fun start(onStepInput: (StepInput, MotionSnapshot?) -> Unit) {
        if (isListening) return
        isListening = true
        callback = onStepInput
        lastDispatchedStepTimeMs = 0L
        gravityBaseline = 9.8f
        filteredMagnitude = 9.8f
        lastAccStepTimeNs = 0L
        waveState = AccWaveState.WAITING_FOR_CREST

        val sm = sensorManager ?: return

        activeTrackingMode = resolvedTrackingMode
        Log.d("WakeWalk", "AndroidSensorAdapter: Starting session with activeTrackingMode=$activeTrackingMode")

        // 1. Always register accelerometer for anti-cheat motion analysis & fallback
        accelerometerSensor?.let {
            sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            Log.d("WakeWalk", "AndroidSensorAdapter: Registered ACCELEROMETER")
        }

        // 2. Select exactly ONE primary hardware step sensor to avoid double counting
        when (activeTrackingMode) {
            StepTrackingMode.HARDWARE_DETECTOR -> {
                stepDetectorSensor?.let {
                    sm.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
                    Log.d("WakeWalk", "AndroidSensorAdapter: Registered sole hardware STEP_DETECTOR")
                }
            }
            StepTrackingMode.HARDWARE_COUNTER -> {
                stepCounterSensor?.let {
                    sm.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
                    Log.d("WakeWalk", "AndroidSensorAdapter: Registered sole hardware STEP_COUNTER")
                }
            }
            StepTrackingMode.ACCELEROMETER_FALLBACK, StepTrackingMode.UNAVAILABLE -> {
                Log.d("WakeWalk", "AndroidSensorAdapter: Using ACCELEROMETER_FALLBACK as primary step source")
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val nowNs = event.timestamp
        val nowMs = System.currentTimeMillis()

        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                Log.d("WakeWalk", "AndroidSensorAdapter: Hardware STEP_DETECTOR fired")
                lastDispatchedStepTimeMs = nowMs
                val input = StepInput.DetectorStep(timestampNs = nowNs)
                callback?.invoke(input, latestMotion)
            }

            Sensor.TYPE_STEP_COUNTER -> {
                val cumulative = event.values[0].toLong()
                Log.d("WakeWalk", "AndroidSensorAdapter: Hardware STEP_COUNTER fired cumulative=$cumulative")
                lastDispatchedStepTimeMs = nowMs
                val input = StepInput.CounterUpdate(timestampNs = nowNs, cumulativeSteps = cumulative)
                callback?.invoke(input, latestMotion)
            }

            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val motion = MotionSnapshot(timestampNs = nowNs, x = x, y = y, z = z)
                latestMotion = motion

                // Determine whether accelerometer should process steps:
                // Case A: Mode is ACCELEROMETER_FALLBACK (no permission or no hardware sensor)
                // Case B: Hardware sensor was registered, but hasn't emitted a step in > 2000ms (watchdog for sluggish/batched hardware)
                val isFallbackMode = activeTrackingMode == StepTrackingMode.ACCELEROMETER_FALLBACK
                val isHardwareSluggish = (activeTrackingMode == StepTrackingMode.HARDWARE_DETECTOR || activeTrackingMode == StepTrackingMode.HARDWARE_COUNTER) &&
                        (nowMs - lastDispatchedStepTimeMs > 2000L)

                if (isFallbackMode || isHardwareSluggish) {
                    processAccelerometerGait(motion)
                }
            }
        }
    }

    /**
     * High-reliability dynamic AC-centered accelerometer step detector.
     * Uses dynamic gravity calibration and wave crest/trough transitions.
     */
    private fun processAccelerometerGait(motion: MotionSnapshot) {
        val rawMag = motion.magnitude

        // 1. Low-pass filter (alpha = 0.35) removes motor vibration and electronic jitter
        filteredMagnitude = 0.65f * filteredMagnitude + 0.35f * rawMag

        // 2. Slow running baseline (beta = 0.03) dynamically tracks device resting gravity
        gravityBaseline = 0.97f * gravityBaseline + 0.03f * filteredMagnitude

        // 3. AC step signal centered at 0.0
        val acSignal = filteredMagnitude - gravityBaseline

        val nowNs = motion.timestampNs

        when (waveState) {
            AccWaveState.WAITING_FOR_CREST -> {
                // Crest threshold (+0.70 m/s² above baseline gravity): foot push-off/heel strike
                if (acSignal > 0.70f) {
                    waveState = AccWaveState.WAITING_FOR_TROUGH
                }
            }
            AccWaveState.WAITING_FOR_TROUGH -> {
                // Trough threshold (-0.60 m/s² below baseline gravity): foot lift / swing phase
                if (acSignal < -0.60f) {
                    val intervalNs = nowNs - lastAccStepTimeNs
                    val intervalMs = intervalNs / 1_000_000L

                    // Valid human walking cadence: 250ms (4 Hz sprint) to 1800ms (0.55 Hz shuffle)
                    if (intervalMs in 250L..1800L || lastAccStepTimeNs == 0L) {
                        lastAccStepTimeNs = nowNs
                        lastDispatchedStepTimeMs = System.currentTimeMillis()
                        Log.d("WakeWalk", "AndroidSensorAdapter: Accelerometer step detected! (interval=${intervalMs}ms, acSignal=${acSignal})")
                        val input = StepInput.AccelerometerCandidate(nowNs, motion)
                        callback?.invoke(input, motion)
                    }
                    waveState = AccWaveState.WAITING_FOR_CREST
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun stop() {
        if (!isListening) return
        isListening = false
        callback = null
        try {
            sensorManager?.unregisterListener(this)
            Log.d("WakeWalk", "AndroidSensorAdapter: Unregistered all sensor listeners")
        } catch (e: Exception) {
            Log.w("WakeWalk", "AndroidSensorAdapter: Error unregistering listeners", e)
        }
    }
}
