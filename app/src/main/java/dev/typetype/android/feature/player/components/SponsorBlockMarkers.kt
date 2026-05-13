package dev.typetype.android.feature.player.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
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
        val radius = height / 2f
        segments.forEach { segment ->
            val startFraction = (segment.startMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            val endFraction = (segment.endMs.toFloat() / durationMs.toFloat()).coerceIn(startFraction, 1f)
            val xStart = startFraction * width
            val xEnd = endFraction * width
            drawRoundRect(
                color = sponsorBlockColorForCategory(segment.category),
                topLeft = Offset(xStart, 0f),
                size = Size((xEnd - xStart).coerceAtLeast(3f), height),
                cornerRadius = CornerRadius(radius, radius),
            )
        }
    }
}

internal fun sponsorBlockColorForCategory(category: SponsorCategory): Color = when (category) {
    SponsorCategory.Sponsor -> Color(0xFF00D400)
    SponsorCategory.SelfPromo -> Color(0xFFFFFF00)
    SponsorCategory.Intro -> Color(0xFF0098FF)
    SponsorCategory.Outro -> Color(0xFF0202ED)
    SponsorCategory.Interaction -> Color(0xFFCC00FF)
    SponsorCategory.Poi -> Color(0xFFFF1684)
    SponsorCategory.Preview -> Color(0xFF008FD6)
    SponsorCategory.MusicOffTopic -> Color(0xFFFF9900)
    SponsorCategory.Filler -> Color(0xFF7300FF)
    SponsorCategory.Unknown -> Color(0xFF9E9E9E)
}
