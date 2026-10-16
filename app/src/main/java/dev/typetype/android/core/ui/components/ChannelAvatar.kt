package dev.typetype.android.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import dev.typetype.android.core.ui.share.LocalServerBaseUrl
import dev.typetype.android.core.ui.share.buildImageUrl
import kotlinx.coroutines.delay

@Composable
fun ChannelAvatar(
    avatarUrl: String,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val serverBaseUrl = LocalServerBaseUrl.current
    val context = LocalPlatformContext.current
    var loaded by remember(serverBaseUrl, avatarUrl) { mutableStateOf(false) }
    val hasSource = avatarUrl.isNotBlank()
    var missingExpired by remember(avatarUrl) { mutableStateOf(false) }

    LaunchedEffect(avatarUrl, hasSource) {
        if (hasSource) {
            missingExpired = false
            return@LaunchedEffect
        }
        missingExpired = false
        delay(MISSING_AVATAR_GRACE_MILLIS)
        missingExpired = true
    }

    val missing = !hasSource && !missingExpired
    val loading = missing || (hasSource && !loaded)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics { contentDescription?.let { this.contentDescription = it } },
        contentAlignment = Alignment.Center,
    ) {
        if (!loading) {
            Text(
                text = avatarInitial(name),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (hasSource) {
            RetryingImage(
                request = ImageRequest.Builder(context)
                    .data(buildImageUrl(serverBaseUrl, avatarUrl))
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onLoaded = { loaded = it },
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (loading) {
            TypeTypeSkeletonCircle(size = size)
        }
    }
}

internal fun avatarRetryUrl(url: String, attempt: Int): String {
    if (attempt <= 0 || url.isBlank()) return url
    val separator = if (url.contains('?')) '&' else '?'
    return "$url$separator$AVATAR_RETRY_PARAM=$attempt"
}

internal fun avatarInitial(name: String): String =
    name.trim().firstOrNull()?.uppercase() ?: "?"

private const val AVATAR_RETRY_PARAM = "_tt_avatar_retry"
private const val MISSING_AVATAR_GRACE_MILLIS = 1_500L
