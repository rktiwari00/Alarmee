package app.wakewalk.ui.settings

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.hardware.SensorManager
import androidx.test.core.app.ApplicationProvider
import app.wakewalk.data.preferences.UserPreferences
import app.wakewalk.data.preferences.UserPreferencesRepository
import app.wakewalk.data.preferences.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var preferencesRepository: UserPreferencesRepository
    private lateinit var alarmManager: AlarmManager
    private lateinit var notificationManager: NotificationManager
    private var sensorManager: SensorManager? = null
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        preferencesRepository = UserPreferencesRepository(context.dataStore)
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

        viewModel = SettingsViewModel(
            preferencesRepository = preferencesRepository,
            context = context,
            alarmManager = alarmManager,
            notificationManager = notificationManager,
            sensorManager = sensorManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitializationDoesNotThrowNpe() {
        val initialStatus = viewModel.diagnosticStatus.value
        assertNotNull(initialStatus)
    }

    @Test
    fun testRefreshDiagnostics() {
        val refreshed = viewModel.refreshDiagnostics()
        assertNotNull(refreshed)
        assertEquals(refreshed, viewModel.diagnosticStatus.value)
    }

    @Test
    fun testUpdatePreferences() = runTest(testDispatcher) {
        viewModel.setEmergencyMethod(UserPreferences.METHOD_LONG_PRESS)
        viewModel.setDefaultStepTarget(250)
        viewModel.setTheme(UserPreferences.THEME_DARK)
        advanceUntilIdle()

        val prefs = preferencesRepository.userPreferencesFlow.first {
            it.emergencyDismissalMethod == UserPreferences.METHOD_LONG_PRESS &&
                    it.defaultStepTarget == 250 &&
                    it.theme == UserPreferences.THEME_DARK
        }
        assertEquals(UserPreferences.METHOD_LONG_PRESS, prefs.emergencyDismissalMethod)
        assertEquals(250, prefs.defaultStepTarget)
        assertEquals(UserPreferences.THEME_DARK, prefs.theme)
    }
}
