package video.typetype.tv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import video.typetype.sdk.core.SavedPlaylist
import video.typetype.sdk.core.Playlist
import video.typetype.sdk.core.UserPlaylist

@Composable
internal fun UserPlaylistRow(playlists: List<UserPlaylist>, onOpen: (UserPlaylist) -> Unit) {
    PlaylistRow(title = "Playlists") {
        items(playlists, key = { it.id }) { playlist ->
            PlaylistCard(
                title = playlist.name,
                subtitle = "${playlist.videoCount} videos",
                imageUrl = playlist.videos.firstOrNull()?.thumbnailUrl,
                onClick = { onOpen(playlist) },
            )
        }
    }
}

@Composable
internal fun SavedPlaylistRow(playlists: List<SavedPlaylist>, onOpen: (SavedPlaylist) -> Unit) {
    PlaylistRow(title = "Saved playlists") {
        items(playlists, key = { it.id }) { playlist ->
            PlaylistCard(
                title = playlist.title,
                subtitle = "${playlist.streamCount} videos",
                imageUrl = playlist.thumbnailUrl,
                onClick = { onOpen(playlist) },
            )
        }
    }
}

@Composable
internal fun ChannelPlaylistRow(playlists: List<Playlist>, onOpen: (Playlist) -> Unit) {
    PlaylistRow(title = "Playlists") {
        items(playlists, key = { it.id }) { playlist ->
            PlaylistCard(
                title = playlist.title,
                subtitle = "${playlist.streamCount} videos",
                imageUrl = playlist.thumbnailUrl,
                onClick = { onOpen(playlist) },
            )
        }
    }
}

@Composable
private fun PlaylistRow(title: String, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        androidx.compose.foundation.layout.Box(Modifier.padding(horizontal = 58.dp)) { SectionTitle(title) }
        LazyRow(
            modifier = Modifier.focusRestorer(),
            contentPadding = PaddingValues(horizontal = 58.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

@Composable
private fun PlaylistCard(title: String, subtitle: String, imageUrl: String?, onClick: () -> Unit) {
    Column(modifier = Modifier.width(184.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(104.dp),
            onClick = onClick,
            shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(104.dp),
            )
        }
        Text(title, maxLines = 1, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
