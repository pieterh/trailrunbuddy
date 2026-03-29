package com.trailrunbuddy.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.trailrunbuddy.app.data.local.entity.TimerGroupEntity

@Dao
interface TimerGroupDao {

    @Insert
    suspend fun insert(group: TimerGroupEntity): Long

    @Query("DELETE FROM timer_groups WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM timer_groups WHERE profile_id = :profileId")
    suspend fun deleteByProfileId(profileId: Long)
}
