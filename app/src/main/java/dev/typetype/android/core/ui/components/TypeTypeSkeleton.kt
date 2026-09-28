package dev.typetype.android.core.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun skeletonPulseAlpha(): Float {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = SKELETON_MIN_ALPHA,
        targetValue = SKELETON_MAX_ALPHA,
        animationSpec = infiniteRepeatable(
            animation = tween(SKELETON_PULSE_MILLIS),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeleton alpha",
    )
    return alpha
}

@Composable
internal fun TypeTypeSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(SKELETON_CORNER),
    alpha: Float = skeletonPulseAlpha(),
) {
    Spacer(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .graphicsLayer { this.alpha = alpha },
    )
}

@Composable
internal fun TypeTypeSkeletonLine(
    widthFraction: Float,
    height: Dp = 10.dp,
    modifier: Modifier = Modifier,
    alpha: Float = skeletonPulseAlpha(),
) {
    TypeTypeSkeleton(
        modifier = modifier.fillMaxWidth(widthFraction).height(height),
        alpha = alpha,
    )
}

@Composable
internal fun TypeTypeSkeletonCircle(
    size: Dp,
    modifier: Modifier = Modifier,
    alpha: Float = skeletonPulseAlpha(),
) {
    TypeTypeSkeleton(
        modifier = modifier.size(size),
        shape = CircleShape,
        alpha = alpha,
    )
}

internal val SkeletonCardShape = RoundedCornerShape(12.dp)

private const val SKELETON_CORNER = 4
private const val SKELETON_MIN_ALPHA = 0.38f
private const val SKELETON_MAX_ALPHA = 0.72f
private const val SKELETON_PULSE_MILLIS = 850
