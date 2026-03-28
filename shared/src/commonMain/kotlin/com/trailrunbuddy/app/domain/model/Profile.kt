package com.trailrunbuddy.app.domain.model

data class Profile(
    val id: Long = 0,
    val name: String,
    val colorHex: String,
    val createdAt: Long = System.currentTimeMillis(),
    val timers: List<Timer> = emptyList(),
    val sortOrder: Int = 0
)
