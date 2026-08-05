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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

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
    widthDp >= TWO_PANE_MIN_WIDTH_DP && heightDp >= TWO_PANE_MIN_HEIGHT_DP ->
        PlayerContentLayoutMode.TwoPane
    else -> PlayerContentLayoutMode.SingleColumn
}

@Composable
internal fun PlayerContentLayout(
    isFullscreen: Boolean,
    modifier: Modifier = Modifier,
    viewport: @Composable (Modifier) -> Unit,
    details: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        when (playerContentLayoutMode(maxWidth.value, maxHeight.value, isFullscreen)) {
            PlayerContentLayoutMode.Fullscreen -> viewport(Modifier.fillMaxSize())
            PlayerContentLayoutMode.SingleColumn -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(PLAYER_SINGLE_COLUMN_LAYOUT_TAG)
                        .verticalScroll(rememberScrollState()),
                ) {
                    viewport(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(VIDEO_ASPECT_RATIO),
                    )
                    details(Modifier.fillMaxWidth())
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
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        viewport(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(VIDEO_ASPECT_RATIO),
                        )
                    }
                    details(
                        Modifier
                            .weight(DETAILS_PANE_WEIGHT)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                    )
                }
            }
        }
    }
}

private const val VIDEO_ASPECT_RATIO = 16f / 9f
private const val TWO_PANE_MIN_WIDTH_DP = 840f
private const val TWO_PANE_MIN_HEIGHT_DP = 480f
private const val PLAYER_PANE_WEIGHT = 1.45f
private const val DETAILS_PANE_WEIGHT = 1f
