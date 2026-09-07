package dev.typetype.android.feature.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
internal fun PlayerLevelOverlay(
    visible: Boolean,
    fraction: Float,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.86f),
        exit = fadeOut() + scaleOut(targetScale = 0.92f),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(128.dp)
                .background(Color.Black.copy(alpha = 0.62f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                drawArc(
                    color = Color.White.copy(alpha = 0.24f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                )
                val clampedFraction = fraction.coerceIn(0f, 1f)
                if (clampedFraction > 0f) {
                    drawArc(
                        color = Color.White,
                        startAngle = -90f,
                        sweepAngle = 360f * clampedFraction,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(64.dp),
            )
        }
    }
}
