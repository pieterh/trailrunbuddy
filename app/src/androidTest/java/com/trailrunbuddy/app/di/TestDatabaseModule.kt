package com.trailrunbuddy.app.di

import android.content.Context
import androidx.room.Room
import com.trailrunbuddy.app.core.di.AppModule
import com.trailrunbuddy.app.data.local.TrailRunBuddyDatabase
import com.trailrunbuddy.app.data.local.dao.ProfileDao
import com.trailrunbuddy.app.data.local.dao.SessionDao
import com.trailrunbuddy.app.data.local.dao.TimerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton
import com.trailrunbuddy.app.core.di.IoDispatcher

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [AppModule::class])
object TestDatabaseModule {

    @Provides
    @Singleton
    fun provideInMemoryDatabase(@ApplicationContext context: Context): TrailRunBuddyDatabase =
        Room.inMemoryDatabaseBuilder(context, TrailRunBuddyDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    @Provides
    fun provideProfileDao(db: TrailRunBuddyDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideTimerDao(db: TrailRunBuddyDatabase): TimerDao = db.timerDao()

    @Provides
    fun provideSessionDao(db: TrailRunBuddyDatabase): SessionDao = db.sessionDao()

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.Unconfined
}
