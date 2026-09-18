package video.typetype.tv.ui

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEnterExitScope
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import video.typetype.sdk.core.Channel
import video.typetype.sdk.core.StreamDetails

internal fun StreamDetails.asChannel(): Channel = Channel(
    id = null,
    name = uploaderName,
    url = uploaderUrl,
    description = "",
    avatarUrl = uploaderAvatarUrl,
    bannerUrl = null,
    subscriberCount = uploaderSubscriberCount,
    streamCount = null,
    isVerified = uploaderVerified,
    videos = emptyList(),
    nextPage = null,
)

@Composable
@OptIn(ExperimentalComposeUiApi::class)
internal fun OverlaySurface(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize()
            .focusProperties { onExit = { scope: FocusEnterExitScope -> scope.cancelFocusChange() } }
            .focusGroup(),
        colors = androidx.tv.material3.SurfaceDefaults.colors(MaterialTheme.colorScheme.background),
    ) { content() }
}

@Composable
internal fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(20.dp),
        shape = RoundedCornerShape(12.dp),
        colors = androidx.tv.material3.SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Text(
            message,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}
