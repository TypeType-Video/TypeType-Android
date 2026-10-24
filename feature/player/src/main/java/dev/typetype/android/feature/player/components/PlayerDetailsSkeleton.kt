package dev.typetype.android.feature.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.typetype.android.core.ui.components.TypeTypeSkeleton
import dev.typetype.android.core.ui.components.TypeTypeSkeletonCircle
import dev.typetype.android.core.ui.components.TypeTypeSkeletonLine
import dev.typetype.android.core.ui.components.skeletonPulseAlpha

@Composable
internal fun PlayerDetailsSkeleton(modifier: Modifier = Modifier) {
    val alpha = skeletonPulseAlpha()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TitleSkeleton(alpha)
        UploaderSkeleton(alpha)
        ActionRowSkeleton(alpha)
        DescriptionSkeleton(alpha)
    }
}

@Composable
private fun TitleSkeleton(alpha: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TypeTypeSkeletonLine(widthFraction = 1f, height = 18.dp, alpha = alpha)
        TypeTypeSkeletonLine(widthFraction = 0.72f, height = 18.dp, alpha = alpha)
    }
}

@Composable
private fun UploaderSkeleton(alpha: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TypeTypeSkeletonCircle(size = 40.dp, alpha = alpha)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            TypeTypeSkeletonLine(widthFraction = 0.42f, height = 12.dp, alpha = alpha)
            TypeTypeSkeletonLine(widthFraction = 0.28f, alpha = alpha)
        }
        TypeTypeSkeleton(
            modifier = Modifier.width(104.dp).height(40.dp),
            alpha = alpha,
        )
    }
}

@Composable
private fun ActionRowSkeleton(alpha: Float) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(PLAYER_ACTION_PLACEHOLDERS) {
            TypeTypeSkeletonCircle(size = 48.dp, alpha = alpha)
        }
    }
}

@Composable
private fun DescriptionSkeleton(alpha: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TypeTypeSkeletonLine(widthFraction = 1f, alpha = alpha)
        TypeTypeSkeletonLine(widthFraction = 0.86f, alpha = alpha)
        TypeTypeSkeletonLine(widthFraction = 0.52f, alpha = alpha)
    }
}

private const val PLAYER_ACTION_PLACEHOLDERS = 6
