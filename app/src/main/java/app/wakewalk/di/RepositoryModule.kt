package app.wakewalk.di

import app.wakewalk.data.repository.AlarmRepositoryImpl
import app.wakewalk.data.repository.StatisticsRepositoryImpl
import app.wakewalk.data.repository.WakeSessionRepositoryImpl
import app.wakewalk.domain.repository.AlarmRepository
import app.wakewalk.domain.repository.StatisticsRepository
import app.wakewalk.domain.repository.WakeSessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAlarmRepository(impl: AlarmRepositoryImpl): AlarmRepository

    @Binds
    @Singleton
    abstract fun bindWakeSessionRepository(impl: WakeSessionRepositoryImpl): WakeSessionRepository

    @Binds
    @Singleton
    abstract fun bindStatisticsRepository(impl: StatisticsRepositoryImpl): StatisticsRepository
}
