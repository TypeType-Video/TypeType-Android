package video.typetype.tv.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import video.typetype.tv.player.TvSponsorBlockSegment

@Composable
internal fun SponsorBlockIndicator(
    segment: TvSponsorBlockSegment,
    canSkip: Boolean,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = androidx.tv.material3.SurfaceDefaults.colors(
            containerColor = Color.Black.copy(alpha = .82f),
            contentColor = Color.White,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = sponsorCategoryColor(segment.category))
            Text(segment.label, style = MaterialTheme.typography.titleSmall)
            if (canSkip) {
                Spacer(Modifier.width(4.dp))
                Button(onClick = onSkip) {
                    Icon(Icons.Default.SkipNext, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Skip")
                }
            }
        }
    }
}

@Composable
internal fun SponsorBlockProgressBar(
    progress: Float,
    durationMilliseconds: Long,
    segments: List<TvSponsorBlockSegment>,
    showSegments: Boolean,
    modifier: Modifier = Modifier,
) {
    val track = Color.White.copy(alpha = .3f)
    val played = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        drawRoundRect(track, size = size, cornerRadius = CornerRadius(size.height / 2f))
        drawRoundRect(
            played,
            size = Size(size.width * progress.coerceIn(0f, 1f), size.height),
            cornerRadius = CornerRadius(size.height / 2f),
        )
        if (showSegments && durationMilliseconds > 0L) {
            segments.forEach { drawSponsorSegment(it, durationMilliseconds) }
        }
    }
}

private fun DrawScope.drawSponsorSegment(segment: TvSponsorBlockSegment, durationMilliseconds: Long) {
    val start = segment.startMilliseconds.toFloat() / durationMilliseconds
    val end = segment.endMilliseconds.toFloat() / durationMilliseconds
    val left = size.width * start.coerceIn(0f, 1f)
    val right = size.width * end.coerceIn(0f, 1f)
    if (right <= left) return
    drawRect(
        color = sponsorCategoryColor(segment.category),
        topLeft = Offset(left, 0f),
        size = Size((right - left).coerceAtLeast(2f), size.height),
    )
}

internal fun sponsorCategoryColor(category: String): Color = when (category) {
    "sponsor" -> Color(0xFF00D400)
    "selfpromo" -> Color(0xFFFFFF00)
    "exclusive_access" -> Color(0xFF008A5C)
    "interaction" -> Color(0xFFCC00FF)
    "poi_highlight" -> Color(0xFFFF1684)
    "intro" -> Color(0xFF00FFFF)
    "outro" -> Color(0xFF0202ED)
    "preview" -> Color(0xFF008FD6)
    "filler" -> Color(0xFF7300FF)
    "chapter" -> Color(0xFFFFD000)
    "music_offtopic" -> Color(0xFFFF9900)
    else -> Color.White.copy(alpha = .7f)
}
