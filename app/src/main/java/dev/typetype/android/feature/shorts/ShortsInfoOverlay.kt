package dev.typetype.android.feature.shorts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.typetype.android.R
import dev.typetype.android.domain.feed.Video

@Composable
internal fun ShortsInfoOverlay(
    video: Video,
    title: String,
    isSubscribed: Boolean,
    subscriptionInFlight: Boolean,
    onOpenChannel: () -> Unit,
    onToggleSubscription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth()
            .padding(start = 20.dp, top = 20.dp, end = 80.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable(role = Role.Button, onClick = onOpenChannel)
                    .padding(vertical = 6.dp),
            ) {
                AsyncImage(
                    model = video.uploaderAvatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(34.dp).clip(CircleShape),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = video.uploaderName,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (video.uploaderUrl.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onToggleSubscription,
                    enabled = !subscriptionInFlight,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSubscribed) {
                            Color.Black.copy(alpha = 0.68f)
                        } else {
                            Color.White
                        },
                        contentColor = if (isSubscribed) Color.White else Color.Black,
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp,
                        vertical = 6.dp,
                    ),
                ) {
                    Text(
                        text = stringResource(
                            if (isSubscribed) R.string.channel_subscribed
                            else R.string.channel_subscribe,
                        ),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
