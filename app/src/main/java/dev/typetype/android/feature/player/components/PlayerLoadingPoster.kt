package dev.typetype.android.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import dev.typetype.android.core.ui.branding.rememberVideoBranding
import dev.typetype.android.core.ui.share.LocalServerBaseUrl
import dev.typetype.android.core.ui.share.buildImageUrl
import dev.typetype.android.domain.stream.Stream

@Composable
internal fun PlayerLoadingPoster(
    stream: Stream,
    modifier: Modifier = Modifier,
) {
    val serverBaseUrl = LocalServerBaseUrl.current
    val branding = rememberVideoBranding(
        sourceUrl = stream.id,
        title = stream.title,
        thumbnailUrl = stream.thumbnailUrl,
        durationSeconds = stream.durationSeconds,
    )
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = buildImageUrl(serverBaseUrl, branding.thumbnailUrl),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)))
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
