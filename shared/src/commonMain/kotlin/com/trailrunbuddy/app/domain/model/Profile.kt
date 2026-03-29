package com.trailrunbuddy.app.domain.model

data class Profile(
    val id: Long = 0,
    val name: String,
    val colorHex: String,
    val createdAt: Long = System.currentTimeMillis(),
    val items: List<ProfileItem> = emptyList(),
    val sortOrder: Int = 0
) {
    val standaloneTimers: List<Timer>
        get() = items.filterIsInstance<ProfileItem.StandaloneTimer>().map { it.timer }

    val group: TimerGroup?
        get() = items.filterIsInstance<ProfileItem.Group>().firstOrNull()?.group

    val allTimers: List<Timer>
        get() = standaloneTimers + (group?.timers ?: emptyList())
}
