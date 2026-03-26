package com.trailrunbuddy.app.core.util

import kotlin.math.absoluteValue

object ColorGenerator {

    private val palette = listOf(
        "#1E88E5", // Blue
        "#43A047", // Green
        "#FB8C00", // Orange
        "#E53935", // Red
        "#8E24AA", // Purple
        "#00ACC1", // Cyan
        "#3949AB", // Indigo
        "#F4511E", // Deep Orange
        "#00897B", // Teal
        "#7CB342"  // Light Green
    )

    fun fromName(name: String): String {
        val index = name.trim().hashCode().absoluteValue % palette.size
        return palette[index]
    }
}
