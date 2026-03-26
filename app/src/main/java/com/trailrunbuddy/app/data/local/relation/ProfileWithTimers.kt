package com.trailrunbuddy.app.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.trailrunbuddy.app.data.local.entity.ProfileEntity
import com.trailrunbuddy.app.data.local.entity.TimerEntity

data class ProfileWithTimers(
    @Embedded val profile: ProfileEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "profile_id"
    )
    val timers: List<TimerEntity>
)
