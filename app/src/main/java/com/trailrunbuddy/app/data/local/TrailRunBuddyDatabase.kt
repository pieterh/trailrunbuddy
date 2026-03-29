package com.trailrunbuddy.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.trailrunbuddy.app.data.local.dao.ProfileDao
import com.trailrunbuddy.app.data.local.dao.SessionDao
import com.trailrunbuddy.app.data.local.dao.TimerDao
import com.trailrunbuddy.app.data.local.dao.TimerGroupDao
import com.trailrunbuddy.app.data.local.entity.ProfileEntity
import com.trailrunbuddy.app.data.local.entity.SessionEntity
import com.trailrunbuddy.app.data.local.entity.TimerEntity
import com.trailrunbuddy.app.data.local.entity.TimerGroupEntity

@Database(
    entities = [ProfileEntity::class, TimerEntity::class, SessionEntity::class, TimerGroupEntity::class],
    version = 4,
    exportSchema = true
)
abstract class TrailRunBuddyDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun timerDao(): TimerDao
    abstract fun sessionDao(): SessionDao
    abstract fun timerGroupDao(): TimerGroupDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profiles ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE timer_groups ADD COLUMN timer_type TEXT NOT NULL DEFAULT 'REPEATING'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS timer_groups (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        profile_id INTEGER NOT NULL,
                        sort_order INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_timer_groups_profile_id ON timer_groups(profile_id)")
                db.execSQL("ALTER TABLE timers ADD COLUMN group_id INTEGER REFERENCES timer_groups(id) ON DELETE SET NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_timers_group_id ON timers(group_id)")
            }
        }
    }
}
