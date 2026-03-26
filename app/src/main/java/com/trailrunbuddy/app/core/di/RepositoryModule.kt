package com.trailrunbuddy.app.core.di

import com.trailrunbuddy.app.data.repository.ProfileRepositoryImpl
import com.trailrunbuddy.app.data.repository.SessionRepositoryImpl
import com.trailrunbuddy.app.data.repository.SettingsRepositoryImpl
import com.trailrunbuddy.app.domain.repository.ProfileRepository
import com.trailrunbuddy.app.domain.repository.SessionRepository
import com.trailrunbuddy.app.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
