package dev.typetype.android.feature.player.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import dev.typetype.android.domain.stream.SponsorBlockSegment
import dev.typetype.android.domain.stream.SponsorCategory

@Composable
fun SponsorBlockMarkers(
    segments: List<SponsorBlockSegment>,
    durationMs: Long,
    modifier: Modifier = Modifier,
) {
    if (segments.isEmpty() || durationMs <= 0) return
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        segments.forEach { segment ->
            val xStart = (segment.startMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) * width
            val xEnd = (segment.endMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) * width
            drawRect(
                color = colorForCategory(segment.category),
                topLeft = Offset(xStart, 0f),
                size = Size((xEnd - xStart).coerceAtLeast(2f), height),
            )
        }
    }
}

private fun colorForCategory(category: SponsorCategory): Color = when (category) {
    SponsorCategory.Sponsor -> Color(0xFF00D400)
    SponsorCategory.SelfPromo -> Color(0xFFFFFF00)
    SponsorCategory.Intro -> Color(0xFF0098FF)
    SponsorCategory.Outro -> Color(0xFF0202ED)
    SponsorCategory.Interaction -> Color(0xFFCC00FF)
    SponsorCategory.Preview -> Color(0xFF008FD6)
    SponsorCategory.MusicOffTopic -> Color(0xFFFF9900)
    SponsorCategory.Filler -> Color(0xFF7300FF)
    SponsorCategory.Unknown -> Color(0xFF9E9E9E)
}
