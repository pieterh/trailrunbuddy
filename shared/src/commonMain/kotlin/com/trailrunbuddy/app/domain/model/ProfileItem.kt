package com.trailrunbuddy.app.domain.model

sealed class ProfileItem {
    abstract val sortOrder: Int

    data class StandaloneTimer(val timer: Timer) : ProfileItem() {
        override val sortOrder: Int get() = timer.sortOrder
    }

    data class Group(val group: TimerGroup) : ProfileItem() {
        override val sortOrder: Int get() = group.sortOrder
    }
}
