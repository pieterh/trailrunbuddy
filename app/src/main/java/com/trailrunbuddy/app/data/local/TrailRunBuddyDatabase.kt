package com.trailrunbuddy.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.trailrunbuddy.app.data.local.dao.ProfileDao
import com.trailrunbuddy.app.data.local.dao.SessionDao
import com.trailrunbuddy.app.data.local.dao.TimerDao
import com.trailrunbuddy.app.data.local.entity.ProfileEntity
import com.trailrunbuddy.app.data.local.entity.SessionEntity
import com.trailrunbuddy.app.data.local.entity.TimerEntity

@Database(
    entities = [ProfileEntity::class, TimerEntity::class, SessionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class TrailRunBuddyDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun timerDao(): TimerDao
    abstract fun sessionDao(): SessionDao
}
