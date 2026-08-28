package dev.typetype.android.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun VideoDurationBadge(durationSeconds: Long, modifier: Modifier = Modifier) {
    if (durationSeconds <= 0L) return
    Text(
        text = formatVideoDuration(durationSeconds),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(Color.Black.copy(alpha = 0.82f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

internal fun formatVideoDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remaining = seconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, remaining)
    } else {
        "%d:%02d".format(minutes, remaining)
    }
}
