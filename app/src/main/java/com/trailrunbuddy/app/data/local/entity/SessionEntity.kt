package com.trailrunbuddy.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "profile_id") val profileId: Long,
    val state: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "paused_at") val pausedAt: Long?,
    @ColumnInfo(name = "total_paused_ms") val totalPausedMs: Long,
    @ColumnInfo(name = "timer_states_json") val timerStatesJson: String
)
