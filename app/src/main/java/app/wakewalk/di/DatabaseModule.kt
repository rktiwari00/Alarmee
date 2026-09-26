package app.wakewalk.di

import android.content.Context
import androidx.room.Room
import app.wakewalk.data.local.WakeWalkDatabase
import app.wakewalk.data.local.dao.ActiveSessionDao
import app.wakewalk.data.local.dao.AlarmDao
import app.wakewalk.data.local.dao.AlarmHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WakeWalkDatabase {
        return Room.databaseBuilder(
            context,
            WakeWalkDatabase::class.java,
            "wakewalk.db"
        )
            .addMigrations(WakeWalkDatabase.MIGRATION_1_2, WakeWalkDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideAlarmDao(db: WakeWalkDatabase): AlarmDao = db.alarmDao()

    @Provides
    fun provideActiveSessionDao(db: WakeWalkDatabase): ActiveSessionDao = db.activeSessionDao()

    @Provides
    fun provideAlarmHistoryDao(db: WakeWalkDatabase): AlarmHistoryDao = db.alarmHistoryDao()
}
