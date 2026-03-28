package com.trailrunbuddy.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.trailrunbuddy.app.data.local.dao.ProfileDao
import com.trailrunbuddy.app.data.local.dao.SessionDao
import com.trailrunbuddy.app.data.local.dao.TimerDao
import com.trailrunbuddy.app.data.local.entity.ProfileEntity
import com.trailrunbuddy.app.data.local.entity.SessionEntity
import com.trailrunbuddy.app.data.local.entity.TimerEntity

@Database(
    entities = [ProfileEntity::class, TimerEntity::class, SessionEntity::class],
    version = 2,
    exportSchema = true
)
abstract class TrailRunBuddyDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun timerDao(): TimerDao
    abstract fun sessionDao(): SessionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profiles ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
