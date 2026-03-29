package com.trailrunbuddy.app.core.di

import android.content.Context
import androidx.room.Room
import com.trailrunbuddy.app.data.local.TrailRunBuddyDatabase
import com.trailrunbuddy.app.data.local.dao.ProfileDao
import com.trailrunbuddy.app.data.local.dao.SessionDao
import com.trailrunbuddy.app.data.local.dao.TimerDao
import com.trailrunbuddy.app.data.local.dao.TimerGroupDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TrailRunBuddyDatabase =
        Room.databaseBuilder(
            context,
            TrailRunBuddyDatabase::class.java,
            "trailrunbuddy.db"
        )
            .addMigrations(TrailRunBuddyDatabase.MIGRATION_1_2, TrailRunBuddyDatabase.MIGRATION_2_3, TrailRunBuddyDatabase.MIGRATION_3_4)
            .build()

    @Provides
    fun provideProfileDao(db: TrailRunBuddyDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideTimerDao(db: TrailRunBuddyDatabase): TimerDao = db.timerDao()

    @Provides
    fun provideSessionDao(db: TrailRunBuddyDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideTimerGroupDao(db: TrailRunBuddyDatabase): TimerGroupDao = db.timerGroupDao()

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
