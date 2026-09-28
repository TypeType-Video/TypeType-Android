package dev.typetype.android.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.typetype.android.core.ui.share.LocalServerBaseUrl
import dev.typetype.android.core.ui.share.buildImageUrl

@Composable
fun SkeletonImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = SkeletonCardShape,
    contentScale: ContentScale = ContentScale.Crop,
    crossfadeMillis: Int = 0,
) {
    val serverBaseUrl = LocalServerBaseUrl.current
    val context = LocalPlatformContext.current
    var loaded by remember(imageUrl) { mutableStateOf(false) }
    val hasSource = imageUrl.isNotBlank()
    Box(modifier = modifier.clip(shape)) {
        if (hasSource && !loaded) {
            TypeTypeSkeleton(modifier = Modifier.matchParentSize(), shape = shape)
        }
        if (hasSource) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(buildImageUrl(serverBaseUrl, imageUrl))
                    .apply { if (crossfadeMillis > 0) crossfade(crossfadeMillis) }
                    .build(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                onSuccess = { loaded = true },
                onError = { loaded = false },
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
