package dev.typetype.android.feature.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.composed
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

internal const val PLAYER_SINGLE_COLUMN_LAYOUT_TAG = "player_single_column_layout"
internal const val PLAYER_TWO_PANE_LAYOUT_TAG = "player_two_pane_layout"

internal enum class PlayerContentLayoutMode {
    Fullscreen,
    SingleColumn,
    TwoPane,
}

internal fun playerContentLayoutMode(
    widthDp: Float,
    heightDp: Float,
    isFullscreen: Boolean,
): PlayerContentLayoutMode = when {
    isFullscreen -> PlayerContentLayoutMode.Fullscreen
    widthDp > heightDp &&
        widthDp >= TWO_PANE_MIN_WIDTH_DP &&
        heightDp >= TWO_PANE_MIN_HEIGHT_DP ->
        PlayerContentLayoutMode.TwoPane
    else -> PlayerContentLayoutMode.SingleColumn
}

@Composable
internal fun PlayerContentLayout(
    isFullscreen: Boolean,
    hostTransitionProgress: Float = 0f,
    modifier: Modifier = Modifier,
    viewport: @Composable (Modifier) -> Unit,
    details: @Composable (Modifier) -> Unit,
) {
    val currentViewport by rememberUpdatedState(viewport)
    val retainedViewport = remember {
        movableContentOf<Modifier> { viewportModifier ->
            currentViewport(viewportModifier)
        }
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        when (playerContentLayoutMode(maxWidth.value, maxHeight.value, isFullscreen)) {
            PlayerContentLayoutMode.Fullscreen -> retainedViewport(Modifier.fillMaxSize())
            PlayerContentLayoutMode.SingleColumn -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(PLAYER_SINGLE_COLUMN_LAYOUT_TAG)
                        .verticalScroll(rememberScrollState()),
                ) {
                    retainedViewport(
                        Modifier
                            .playerViewportTransition(hostTransitionProgress)
                            .fillMaxWidth()
                            .aspectRatio(VIDEO_ASPECT_RATIO)
                            .testTag(PLAYER_VIEWPORT_TAG),
                    )
                    details(
                        Modifier
                            .fillMaxWidth()
                            .playerDetailsTransition(hostTransitionProgress),
                    )
                }
            }
            PlayerContentLayoutMode.TwoPane -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(PLAYER_TWO_PANE_LAYOUT_TAG),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(PLAYER_PANE_WEIGHT)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopStart,
                    ) {
                        retainedViewport(
                            Modifier
                                .playerViewportTransition(hostTransitionProgress)
                                .fillMaxWidth()
                                .aspectRatio(VIDEO_ASPECT_RATIO)
                                .testTag(PLAYER_VIEWPORT_TAG),
                        )
                    }
                    details(
                        Modifier
                            .weight(DETAILS_PANE_WEIGHT)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .playerDetailsTransition(hostTransitionProgress),
                    )
                }
            }
        }
    }
}

private fun Modifier.playerViewportTransition(progress: Float): Modifier = composed {
    val fraction = progress.coerceIn(0f, 1f)
    val density = LocalDensity.current
    val miniWidthPx = with(density) { MINI_VIDEO_WIDTH.toPx() }
    val miniHeightPx = with(density) { MINI_VIDEO_HEIGHT.toPx() }
    val miniStartPx = with(density) { MINI_VIDEO_START.toPx() }
    val miniTopPx = with(density) { MINI_VIDEO_TOP.toPx() }
    val shape = RoundedCornerShape(MINI_VIDEO_CORNER * fraction)
    graphicsLayer {
        val targetScaleX = miniWidthPx / size.width.coerceAtLeast(1f)
        val targetScaleY = miniHeightPx / size.height.coerceAtLeast(1f)
        scaleX = 1f + (targetScaleX - 1f) * fraction
        scaleY = 1f + (targetScaleY - 1f) * fraction
        translationX = miniStartPx * fraction
        translationY = miniTopPx * fraction
        transformOrigin = TransformOrigin(0f, 0f)
        this.shape = shape
        clip = fraction > 0f
    }
}

private fun Modifier.playerDetailsTransition(progress: Float): Modifier {
    val fraction = progress.coerceIn(0f, 1f)
    return graphicsLayer { alpha = 1f - fraction }
        .then(if (fraction >= 0.99f) Modifier.clearAndSetSemantics { } else Modifier)
}

private const val VIDEO_ASPECT_RATIO = 16f / 9f
private const val TWO_PANE_MIN_WIDTH_DP = 840f
private const val TWO_PANE_MIN_HEIGHT_DP = 480f
private const val PLAYER_PANE_WEIGHT = 1.45f
private const val DETAILS_PANE_WEIGHT = 1f
private val MINI_VIDEO_WIDTH = 80.dp
private val MINI_VIDEO_HEIGHT = 45.dp
private val MINI_VIDEO_START = 8.dp
private val MINI_VIDEO_TOP = 9.dp
private val MINI_VIDEO_CORNER = 6.dp
internal const val PLAYER_VIEWPORT_TAG = "player_viewport"
