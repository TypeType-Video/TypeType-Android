package dev.typetype.android.feature.settings.recommender

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

internal data class DonutPart(val id: String, val label: String, val value: Double, val neutral: Boolean = false)

@Composable
internal fun Donut(
    parts: List<DonutPart>,
    centerValue: Int,
    centerLabel: String,
    unit: (@Composable (Int) -> String)? = null,
    onSelect: ((String) -> Unit)? = null,
) {
    val shades = donutShades()
    val neutral = MaterialTheme.colorScheme.outlineVariant
    val colors = parts.mapIndexed { index, part -> if (part.neutral) neutral else shades[index % shades.size] }
    val total = parts.sumOf { it.value.coerceAtLeast(0.0) }
    val shares = parts.map { if (total > 0) it.value.coerceAtLeast(0.0) / total else 0.0 }
    var active by remember { mutableStateOf<String?>(null) }
    val drawn = remember { Animatable(0f) }
    LaunchedEffect(Unit) { drawn.animateTo(1f, tween(DRAW_MS, easing = FastOutSlowInEasing)) }
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val focusedIndex = parts.indexOfFirst { it.id == active }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 12.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            Canvas(modifier = Modifier.size(RING_SIZE)) {
                val stroke = RING_WIDTH.toPx()
                if (total <= 0) drawCircle(track, radius = (size.minDimension - stroke) / 2, style = Stroke(stroke))
                val gap = if (shares.count { it > 0 } > 1) GAP_DEGREES else 0f
                var start = -90f
                shares.forEachIndexed { index, share ->
                    val sweep = (share * 360f).toFloat()
                    if (sweep > 0f) {
                        val dimmed = active != null && index != focusedIndex
                        drawArc(
                            color = colors[index].copy(alpha = if (dimmed) 0.35f else 1f),
                            startAngle = start + gap / 2,
                            sweepAngle = ((sweep - gap).coerceAtLeast(0.5f)) * drawn.value,
                            useCenter = false,
                            topLeft = Offset(stroke / 2, stroke / 2),
                            size = Size(size.width - stroke, size.height - stroke),
                            style = Stroke(if (index == focusedIndex) stroke * 1.25f else stroke, cap = StrokeCap.Butt),
                        )
                    }
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 40.dp)) {
                val focused = parts.getOrNull(focusedIndex)
                Text(
                    if (focused != null) formatPercent(shares[focusedIndex]) else formatNumber((centerValue * drawn.value).roundToInt().toDouble()),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    focused?.label ?: centerLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column {
            parts.forEachIndexed { index, part ->
                if (shares[index] <= 0.0) return@forEachIndexed
                LegendRow(
                    part = part,
                    share = shares[index],
                    color = colors[index],
                    dimmed = active != null && active != part.id,
                    unit = unit,
                    onClick = {
                        if (onSelect != null) onSelect(part.id) else active = part.id.takeUnless { it == active }
                    },
                    navigates = onSelect != null,
                )
            }
        }
    }
}

@Composable
private fun LegendRow(
    part: DonutPart,
    share: Double,
    color: Color,
    dimmed: Boolean,
    unit: (@Composable (Int) -> String)?,
    onClick: () -> Unit,
    navigates: Boolean,
) {
    val alpha by animateFloatAsState(if (dimmed) 0.45f else 1f, label = "legend")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .alpha(alpha)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(part.label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (unit != null) {
            Text(unit(part.value.toInt()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(formatPercent(share), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (navigates) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun donutShades(): List<Color> {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    return SHADE_MIX.map { lerp(primary, surface, it) }
}

private val SHADE_MIX = listOf(0f, 0.25f, 0.45f, 0.62f, 0.12f, 0.35f, 0.55f)
private val RING_SIZE = 200.dp
private val RING_WIDTH = 22.dp
private const val GAP_DEGREES = 2.5f
private const val DRAW_MS = 700
