package app.wakewalk.di

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.hardware.SensorManager
import android.media.AudioManager
import app.wakewalk.alarm.scheduler.AlarmScheduler
import app.wakewalk.alarm.scheduler.AndroidAlarmScheduler
import app.wakewalk.data.preferences.UserPreferencesRepository
import app.wakewalk.data.preferences.dataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindAlarmScheduler(impl: AndroidAlarmScheduler): AlarmScheduler

    companion object {
        @Provides
        @Singleton
        fun provideAlarmManager(@ApplicationContext context: Context): AlarmManager {
            return context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        }

        @Provides
        @Singleton
        fun provideNotificationManager(@ApplicationContext context: Context): NotificationManager {
            return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }

        @Provides
        @Singleton
        fun provideSensorManager(@ApplicationContext context: Context): SensorManager? {
            return context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        }

        @Provides
        @Singleton
        fun provideAudioManager(@ApplicationContext context: Context): AudioManager {
            return context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }

        @Provides
        @Singleton
        fun provideUserPreferencesRepository(@ApplicationContext context: Context): UserPreferencesRepository {
            return UserPreferencesRepository(context.dataStore)
        }
    }
}
