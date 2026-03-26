package com.trailrunbuddy.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.trailrunbuddy.app.core.util.TimeFormatter

@Composable
fun CountdownDisplay(
    remainingMs: Long,
    isPreWarning: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displaySmall
) {
    val color = if (isPreWarning) {
        MaterialTheme.colorScheme.error
    } else {
        Color.Unspecified
    }

    Text(
        text = TimeFormatter.formatHhMmSsFromMs(remainingMs),
        style = style,
        color = color,
        modifier = modifier
    )
}
