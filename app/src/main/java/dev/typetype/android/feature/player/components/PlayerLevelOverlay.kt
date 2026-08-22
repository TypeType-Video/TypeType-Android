package dev.typetype.android.feature.player.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.typetype.android.feature.player.state.GestureSide
import kotlin.math.abs
import kotlin.math.sin

@Composable
internal fun PlayerLevelOverlay(
    visible: Boolean,
    fraction: Float,
    label: String,
    icon: ImageVector,
    side: GestureSide,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(80),
        label = "playerLevel",
    )
    val edgeColor = Color.Black.copy(alpha = 0.58f)
    val gradient = if (side == GestureSide.Left) {
        Brush.horizontalGradient(listOf(edgeColor, Color.Transparent))
    } else {
        Brush.horizontalGradient(listOf(Color.Transparent, edgeColor))
    }
    Column(
        modifier = modifier
            .width(92.dp)
            .fillMaxHeight(0.72f)
            .background(gradient)
            .padding(horizontal = 18.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
        VerticalLevelMeter(
            fraction = animatedFraction,
            activeColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f).width(32.dp),
        )
        Text(
            text = "${(animatedFraction * 100).toInt()}%",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun VerticalLevelMeter(
    fraction: Float,
    activeColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val bars = 24
        val spacing = size.height / bars
        val activeBars = (bars * fraction).toInt()
        repeat(bars) { index ->
            val active = index < activeBars
            val y = size.height - spacing * (index + 0.5f)
            val wave = 0.45f + abs(sin(index * 0.72f)) * 0.55f
            val halfWidth = size.width * wave / 2f
            drawLine(
                color = if (active) activeColor else Color.White.copy(alpha = 0.22f),
                start = Offset(size.width / 2f - halfWidth, y),
                end = Offset(size.width / 2f + halfWidth, y),
                strokeWidth = if (active) 3.dp.toPx() else 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}
