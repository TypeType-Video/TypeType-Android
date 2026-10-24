package dev.typetype.android.feature.player.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.typetype.android.core.ui.components.TypeTypeSkeletonCircle
import dev.typetype.android.core.ui.components.TypeTypeSkeletonLine
import dev.typetype.android.core.ui.components.skeletonPulseAlpha

@Composable
internal fun CommentSkeleton(avatarSize: Dp = 36.dp) {
    val alpha = skeletonPulseAlpha()
    Row(modifier = Modifier.fillMaxWidth()) {
        TypeTypeSkeletonCircle(size = avatarSize, alpha = alpha)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            TypeTypeSkeletonLine(widthFraction = 0.38f, height = 12.dp, alpha = alpha)
            Spacer(Modifier.height(10.dp))
            TypeTypeSkeletonLine(widthFraction = 1f, alpha = alpha)
            Spacer(Modifier.height(6.dp))
            TypeTypeSkeletonLine(widthFraction = 0.7f, alpha = alpha)
        }
    }
}
