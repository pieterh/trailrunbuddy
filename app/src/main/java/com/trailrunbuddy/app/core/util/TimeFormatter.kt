package com.trailrunbuddy.app.core.util

object TimeFormatter {

    fun formatHhMmSs(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }

    fun formatHhMmSsFromMs(totalMs: Long): String =
        formatHhMmSs((totalMs / 1000).coerceAtLeast(0))

    fun formatMmSs(totalSeconds: Long): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }
}
