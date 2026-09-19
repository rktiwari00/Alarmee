package app.wakewalk.ui.settings

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.wakewalk.data.preferences.UserPreferences
import app.wakewalk.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiagnosticStatus(
    val exactAlarmsAllowed: Boolean,
    val notificationsEnabled: Boolean,
    val hardwareStepSensorAvailable: Boolean,
    val batteryOptimizationIgnored: Boolean
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
    private val notificationManager: NotificationManager,
    private val sensorManager: SensorManager?
) : ViewModel() {

    val preferencesFlow: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    private val _diagnosticStatus = MutableStateFlow(checkDiagnostics())
    val diagnosticStatus: StateFlow<DiagnosticStatus> = _diagnosticStatus.asStateFlow()

    private fun checkDiagnostics(): DiagnosticStatus {
        val exactAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        val notifications = NotificationManagerCompat.from(context).areNotificationsEnabled()

        val stepSensor = sensorManager?.let { sm ->
            sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null ||
                    sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null
        } ?: false

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val batteryIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
            } catch (_: Throwable) {
                false
            }
        } else {
            true
        }

        return DiagnosticStatus(
            exactAlarmsAllowed = exactAlarms,
            notificationsEnabled = notifications,
            hardwareStepSensorAvailable = stepSensor,
            batteryOptimizationIgnored = batteryIgnored
        )
    }

    fun refreshDiagnostics(): DiagnosticStatus {
        val status = checkDiagnostics()
        _diagnosticStatus.value = status
        return status
    }

    fun setEmergencyMethod(method: String) {
        viewModelScope.launch {
            preferencesRepository.setEmergencyDismissalMethod(method)
        }
    }

    fun setDefaultStepTarget(target: Int) {
        viewModelScope.launch {
            preferencesRepository.setDefaultStepTarget(target)
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            preferencesRepository.setTheme(theme)
        }
    }
}
