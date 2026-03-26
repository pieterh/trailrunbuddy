package com.trailrunbuddy.app.platform.notification

interface SessionNotificationController {
    fun createChannel()
    fun update(profileId: Long, nextTimerName: String, remainingMs: Long)
}
