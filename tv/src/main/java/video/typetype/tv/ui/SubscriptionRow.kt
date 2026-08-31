package video.typetype.tv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import video.typetype.sdk.core.Channel
import video.typetype.sdk.core.Subscription

@Composable
internal fun SubscriptionRow(subscriptions: List<Subscription>, onOpen: (Channel) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        androidx.compose.foundation.layout.Box(Modifier.padding(horizontal = 58.dp)) { SectionTitle("Channels") }
        LazyRow(
            modifier = Modifier.focusRestorer(),
            contentPadding = PaddingValues(horizontal = 58.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items(subscriptions, key = { it.channelUrl }) { subscription ->
                Column(
                    modifier = Modifier.width(142.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Surface(
                        modifier = Modifier.width(112.dp).height(112.dp),
                        onClick = { onOpen(subscription.toChannel()) },
                        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(CircleShape),
                    ) {
                        AsyncImage(
                            model = subscription.avatarUrl,
                            contentDescription = subscription.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.width(112.dp).height(112.dp).clip(CircleShape),
                        )
                    }
                    Text(subscription.name, maxLines = 1, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

private fun Subscription.toChannel(): Channel = Channel(
    id = null,
    name = name,
    url = channelUrl,
    description = "",
    avatarUrl = avatarUrl,
    bannerUrl = null,
    subscriberCount = 0L,
    streamCount = null,
    isVerified = false,
    videos = emptyList(),
    nextPage = null,
)
