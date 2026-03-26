package com.trailrunbuddy.app.platform.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.trailrunbuddy.app.MainActivity
import com.trailrunbuddy.app.core.util.TimeFormatter
import com.trailrunbuddy.app.platform.service.SessionService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "trail_run_session"
        const val NOTIFICATION_ID = 101
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Trail Run Session",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows active session countdown"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun buildNotification(
        profileId: Long,
        nextTimerName: String,
        remainingMs: Long
    ): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("profileId", profileId)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, SessionService::class.java).apply {
            action = SessionService.ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            context, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remainingText = TimeFormatter.formatHhMmSsFromMs(remainingMs)

        return android.app.Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Trail Run in Progress")
            .setContentText("$nextTimerName: $remainingText")
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop",
                stopPendingIntent
            )
            .build()
    }

    fun update(profileId: Long, nextTimerName: String, remainingMs: Long) {
        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification(profileId, nextTimerName, remainingMs)
        )
    }
}
