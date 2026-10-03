package dev.typetype.android.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import kotlinx.coroutines.delay

@Composable
internal fun RetryingImage(
    request: ImageRequest,
    contentDescription: String?,
    contentScale: ContentScale,
    modifier: Modifier,
    onLoaded: (Boolean) -> Unit,
) {
    var attempt by remember(request.data) { mutableIntStateOf(0) }
    var failed by remember(request.data, attempt) { mutableStateOf(false) }
    LaunchedEffect(request.data, attempt, failed) {
        if (failed) {
            delay(imageRetryDelayMillis(attempt))
            attempt += 1
        }
    }
    key(request.data, attempt) {
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            contentScale = contentScale,
            onSuccess = { onLoaded(true) },
            onError = {
                onLoaded(false)
                failed = true
            },
            modifier = modifier,
        )
    }
}

internal fun imageRetryDelayMillis(attempt: Int): Long =
    1_000L shl attempt.coerceIn(0, 5)
