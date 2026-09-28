package dev.typetype.android.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.typetype.android.R

@Composable
internal fun VideoCardSkeleton() {
    val alpha = skeletonPulseAlpha()
    Column(modifier = Modifier.fillMaxWidth()) {
        TypeTypeSkeleton(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            shape = SkeletonCardShape,
            alpha = alpha,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TypeTypeSkeletonCircle(size = 36.dp, alpha = alpha)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                TypeTypeSkeletonLine(widthFraction = 1f, height = 12.dp, alpha = alpha)
                TypeTypeSkeletonLine(widthFraction = 0.72f, height = 12.dp, alpha = alpha)
                TypeTypeSkeletonLine(widthFraction = 0.45f, alpha = alpha)
            }
        }
    }
}

@Composable
internal fun RelatedVideoCardSkeleton(thumbnailWidth: Dp = 148.dp) {
    val alpha = skeletonPulseAlpha()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TypeTypeSkeleton(
            modifier = Modifier.width(thumbnailWidth).aspectRatio(16f / 9f),
            shape = SkeletonCardShape,
            alpha = alpha,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TypeTypeSkeletonLine(widthFraction = 1f, height = 10.dp, alpha = alpha)
            TypeTypeSkeletonLine(widthFraction = 0.75f, height = 10.dp, alpha = alpha)
            TypeTypeSkeletonLine(widthFraction = 0.34f, height = 10.dp, alpha = alpha)
        }
    }
}

@Composable
fun VideoGridSkeleton(
    modifier: Modifier = Modifier,
    count: Int = DEFAULT_SKELETON_CARDS,
    minCellWidth: Dp = 280.dp,
    horizontalPadding: Dp = 16.dp,
    verticalPadding: Dp = 16.dp,
    verticalSpacing: Dp = 24.dp,
    contentDescription: String? = null,
) {
    val description = contentDescription ?: stringResource(R.string.state_loading)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = minCellWidth),
        modifier = modifier
            .fillMaxSize()
            .semantics { this.contentDescription = description },
        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = verticalPadding),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        userScrollEnabled = false,
    ) {
        items(SKELETON_CARD_SLOTS.take(count), key = { it }) { VideoCardSkeleton() }
    }
}

private const val DEFAULT_SKELETON_CARDS = 6
private val SKELETON_CARD_SLOTS = (0 until 12).toList()
