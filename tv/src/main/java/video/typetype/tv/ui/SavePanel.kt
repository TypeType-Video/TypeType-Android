package video.typetype.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import video.typetype.sdk.core.UserPlaylist
import video.typetype.sdk.core.Video

@Composable
internal fun SavePanel(
    video: Video,
    playlists: List<UserPlaylist>,
    inWatchLater: Boolean,
    onToggleWatchLater: () -> Unit,
    onTogglePlaylist: (UserPlaylist, Video) -> Unit,
    onDismiss: () -> Unit,
) {
    val watchLaterFocus = remember { FocusRequester() }
    val playlistFocus = remember(playlists.map(UserPlaylist::id)) {
        playlists.associate { it.id to FocusRequester() }
    }
    var pendingFocus by remember { mutableStateOf<String?>(null) }
    BackHandler(onBack = onDismiss)
    LaunchedEffect(Unit) { watchLaterFocus.requestFocus() }
    LaunchedEffect(inWatchLater, playlists) {
        pendingFocus?.let { key ->
            if (key == WATCH_LATER_KEY) watchLaterFocus else playlistFocus[key]
        }?.requestFocus()
        pendingFocus = null
    }
    Surface(
        modifier = Modifier.width(560.dp).fillMaxHeight(),
        colors = androidx.tv.material3.SurfaceDefaults.colors(containerColor = Color(0xFF111316)),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 28.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Save to", color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Button(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text("Close")
                    }
                }
            }
            item {
                SaveDestination(
                    title = "Watch later",
                    selected = inWatchLater,
                    modifier = Modifier.focusRequester(watchLaterFocus),
                    onClick = {
                        pendingFocus = WATCH_LATER_KEY
                        onToggleWatchLater()
                    },
                )
            }
            items(playlists, key = { it.id }) { playlist ->
                SaveDestination(
                    title = playlist.name,
                    selected = playlist.videos.any { it.url == video.url },
                    modifier = playlistFocus[playlist.id]?.let { Modifier.focusRequester(it) } ?: Modifier,
                    onClick = {
                        pendingFocus = playlist.id
                        onTogglePlaylist(playlist, video)
                    },
                )
            }
            if (playlists.isEmpty()) item {
                Text(
                    "Your TypeType playlists will appear here.",
                    color = Color.White.copy(alpha = .65f),
                    modifier = Modifier.padding(vertical = 18.dp),
                )
            }
        }
    }
}

@Composable
private fun SaveDestination(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Icon(if (selected) Icons.Default.Check else Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Text(title, modifier = Modifier.weight(1f), maxLines = 1)
        Text(if (selected) "Saved" else "Add")
    }
}

private const val WATCH_LATER_KEY = "watch-later"
