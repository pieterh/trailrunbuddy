package com.trailrunbuddy.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.trailrunbuddy.app.data.local.entity.ProfileEntity
import com.trailrunbuddy.app.data.local.relation.ProfileWithTimers
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Transaction
    @Query("SELECT * FROM profiles ORDER BY created_at DESC")
    fun observeAll(): Flow<List<ProfileWithTimers>>

    @Transaction
    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: Long): ProfileWithTimers?

    @Insert
    suspend fun insert(profile: ProfileEntity): Long

    @Update
    suspend fun update(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteById(id: Long)
}
