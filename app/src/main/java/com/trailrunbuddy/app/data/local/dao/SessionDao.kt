package com.trailrunbuddy.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trailrunbuddy.app.data.local.entity.SessionEntity

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions WHERE id = 1")
    suspend fun get(): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = 1")
    suspend fun delete()
}
