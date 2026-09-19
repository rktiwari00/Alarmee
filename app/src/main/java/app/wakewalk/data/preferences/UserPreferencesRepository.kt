package app.wakewalk.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wakewalk_preferences")

data class UserPreferences(
    val emergencyDismissalMethod: String = METHOD_TYPING_PHRASE,
    val defaultStepTarget: Int = 150,
    val theme: String = THEME_SYSTEM,
    val onboardingCompleted: Boolean = false
) {
    companion object {
        const val METHOD_TYPING_PHRASE = "TYPING_PHRASE"
        const val METHOD_LONG_PRESS = "LONG_PRESS_5S"

        const val THEME_SYSTEM = "SYSTEM"
        const val THEME_LIGHT = "LIGHT"
        const val THEME_DARK = "DARK"
    }
}

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    private object PreferencesKeys {
        val EMERGENCY_METHOD = stringPreferencesKey("emergency_dismissal_method")
        val DEFAULT_STEP_TARGET = intPreferencesKey("default_step_target")
        val THEME = stringPreferencesKey("app_theme")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data.map { preferences ->
        UserPreferences(
            emergencyDismissalMethod = preferences[PreferencesKeys.EMERGENCY_METHOD] ?: UserPreferences.METHOD_TYPING_PHRASE,
            defaultStepTarget = preferences[PreferencesKeys.DEFAULT_STEP_TARGET] ?: 150,
            theme = preferences[PreferencesKeys.THEME] ?: UserPreferences.THEME_SYSTEM,
            onboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        )
    }

    suspend fun setEmergencyDismissalMethod(method: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.EMERGENCY_METHOD] = method
        }
    }

    suspend fun setDefaultStepTarget(target: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_STEP_TARGET] = target.coerceIn(50, 2000)
        }
    }

    suspend fun setTheme(theme: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }
}
