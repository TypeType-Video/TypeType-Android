package dev.typetype.android.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.typetype.android.core.ui.theme.LocalTypeTypeAppearance
import dev.typetype.android.domain.preferences.MangaHeadlineMarker

@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    val appearance = LocalTypeTypeAppearance.current
    if (!appearance.isManga || appearance.headlineMarker == MangaHeadlineMarker.None) {
        PlainSectionHeader(text, modifier)
        return
    }
    when (appearance.headlineMarker) {
        MangaHeadlineMarker.Stamp -> StampedSectionHeader(text, modifier, appearance.starburst)
        MangaHeadlineMarker.SpeedLines -> SpeedLineSectionHeader(text, modifier, appearance.speedLines)
        MangaHeadlineMarker.None -> Unit
    }
}

@Composable
private fun PlainSectionHeader(text: String, modifier: Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun StampedSectionHeader(text: String, modifier: Modifier, starburst: Boolean) {
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        if (starburst) MangaStarburst(Modifier.size(48.dp).align(Alignment.CenterEnd))
        Surface(
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
            shape = MaterialTheme.shapes.extraSmall,
        ) {
            Text(
                text = text.uppercase(),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
private fun SpeedLineSectionHeader(text: String, modifier: Modifier, speedLines: Boolean) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (speedLines) MangaSpeedLines(Modifier.size(width = 54.dp, height = 18.dp))
    }
}

@Composable
private fun MangaSpeedLines(modifier: Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(modifier) {
        repeat(5) { index ->
            val y = size.height * (index + 1) / 6f
            drawLine(color, Offset(0f, y), Offset(size.width * (1f - index * 0.08f), y), 2.dp.toPx())
        }
    }
}

@Composable
private fun MangaStarburst(modifier: Modifier) {
    val color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        repeat(12) { index ->
            val angle = index * Math.PI.toFloat() / 6f
            val end = Offset(
                center.x + kotlin.math.cos(angle) * size.width / 2f,
                center.y + kotlin.math.sin(angle) * size.height / 2f,
            )
            drawLine(color, center, end, 1.5.dp.toPx())
        }
    }
}
