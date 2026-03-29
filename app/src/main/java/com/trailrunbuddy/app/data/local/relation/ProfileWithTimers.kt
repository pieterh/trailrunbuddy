package com.trailrunbuddy.app.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.trailrunbuddy.app.data.local.entity.ProfileEntity
import com.trailrunbuddy.app.data.local.entity.TimerEntity
import com.trailrunbuddy.app.data.local.entity.TimerGroupEntity

data class ProfileWithTimers(
    @Embedded val profile: ProfileEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "profile_id",
        entity = TimerGroupEntity::class
    )
    val groups: List<TimerGroupWithTimers>,
    @Relation(
        parentColumn = "id",
        entityColumn = "profile_id"
    )
    val timers: List<TimerEntity>
)

data class TimerGroupWithTimers(
    @Embedded val group: TimerGroupEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "group_id"
    )
    val timers: List<TimerEntity>
)
