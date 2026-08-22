package dev.typetype.android.feature.player.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun CommentSkeleton(avatarSize: Dp = 36.dp) {
    val transition = rememberInfiniteTransition(label = "comment skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.38f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "comment skeleton alpha",
    )
    val color = MaterialTheme.colorScheme.surfaceVariant
    Row(modifier = Modifier.fillMaxWidth().graphicsLayer { this.alpha = alpha }) {
        Spacer(Modifier.size(avatarSize).background(color, CircleShape))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Spacer(
                Modifier.fillMaxWidth(0.38f).height(12.dp)
                    .background(color, RoundedCornerShape(4.dp)),
            )
            Spacer(Modifier.height(10.dp))
            Spacer(
                Modifier.fillMaxWidth().height(10.dp)
                    .background(color, RoundedCornerShape(4.dp)),
            )
            Spacer(Modifier.height(6.dp))
            Spacer(
                Modifier.fillMaxWidth(0.7f).height(10.dp)
                    .background(color, RoundedCornerShape(4.dp)),
            )
        }
    }
}
