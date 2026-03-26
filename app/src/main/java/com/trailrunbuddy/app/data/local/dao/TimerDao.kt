package com.trailrunbuddy.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.trailrunbuddy.app.data.local.entity.TimerEntity

@Dao
interface TimerDao {

    @Insert
    suspend fun insertAll(timers: List<TimerEntity>): List<Long>

    @Insert
    suspend fun insert(timer: TimerEntity): Long

    @Update
    suspend fun update(timer: TimerEntity)

    @Query("DELETE FROM timers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM timers WHERE profile_id = :profileId")
    suspend fun deleteByProfileId(profileId: Long)
}
