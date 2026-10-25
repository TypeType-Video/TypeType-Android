package dev.typetype.android.feature.player.components

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import dev.typetype.android.feature.player.state.ResizeMode

@OptIn(UnstableApi::class)
@Composable
internal fun ShortsVideoSurface(player: Player, resizeMode: ResizeMode, modifier: Modifier = Modifier) {
    ContentFrame(
        player = player,
        modifier = modifier,
        surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
        contentScale = when (resizeMode) {
            ResizeMode.Fit -> ContentScale.Fit
            ResizeMode.Crop -> ContentScale.Crop
            ResizeMode.Stretch -> ContentScale.FillBounds
        },
        keepContentOnReset = true,
        shutter = {},
    )
}
