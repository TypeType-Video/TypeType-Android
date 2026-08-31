package video.typetype.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import video.typetype.sdk.core.StreamAudio
import video.typetype.sdk.core.StreamDetails
import video.typetype.sdk.core.StreamVideo
import video.typetype.sdk.core.isSabrPlaybackTrack

@Composable
internal fun DetailsPlaybackPanel(
    stream: StreamDetails,
    supportedVideoItags: Set<Int>,
    selectedVideoItag: Int?,
    selectedAudioItag: Int?,
    selectedAudioTrackId: String?,
    selectedSubtitleLanguage: String?,
    selectedSubtitleAuto: Boolean,
    selectedSubtitleName: String?,
    onSelectVideoTrack: (Int) -> Unit,
    onSelectAudioTrack: (Int, String?) -> Unit,
    onSelectSubtitle: (String?, Boolean, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val videoTracks = stream.videoOnlyStreams.filter {
        it.isSabrPlaybackTrack() && it.itag in supportedVideoItags
    }.distinctBy { it.itag }
    val selectedVideoIndex = videoTracks.indexOfFirst { it.itag == selectedVideoItag }.coerceAtLeast(0)
    val firstOptionFocus = remember(videoTracks, selectedVideoItag) { FocusRequester() }
    val closeFocus = remember { FocusRequester() }
    LaunchedEffect(videoTracks, selectedVideoItag) {
        if (videoTracks.isEmpty()) closeFocus.requestFocus() else firstOptionFocus.requestFocus()
    }
    BackHandler(onBack = onDismiss)
    Box(
        Modifier.fillMaxSize().background(
            Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .68f), Color.Black.copy(alpha = .96f))),
        ),
    ) {
        Surface(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(660.dp),
            colors = androidx.tv.material3.SurfaceDefaults.colors(
                containerColor = Color(0xFA111318),
                contentColor = Color.White,
            ),
        ) {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 44.dp, vertical = 38.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Playback options", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                            Text("Quality, audio and subtitles", color = Color(0xFFB8BBC2))
                        }
                        Spacer(Modifier.weight(1f))
                        Button(modifier = Modifier.focusRequester(closeFocus), onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }
                item {
                    TrackSection("Video quality") {
                        VideoOptions(
                            videoTracks,
                            selectedVideoItag,
                            selectedVideoIndex,
                            firstOptionFocus,
                            onSelectVideoTrack,
                        )
                    }
                }
                item {
                    TrackSection("Audio") {
                        AudioOptions(
                            stream.audioStreams.filter { it.isSabrPlaybackTrack() }.distinctBy { it.itag },
                            selectedAudioItag,
                            selectedAudioTrackId,
                            onSelectAudioTrack,
                        )
                    }
                }
                if (stream.subtitles.isNotEmpty()) item {
                    TrackSection("Subtitles") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                Button(onClick = { onSelectSubtitle(null, false, null) }) {
                                    Text(if (selectedSubtitleLanguage == null) "Off · selected" else "Off")
                                }
                            }
                            items(stream.subtitles.distinctBy { Triple(it.languageTag, it.isAutoGenerated, it.name) }) { item ->
                                val selected = item.languageTag == selectedSubtitleLanguage &&
                                    item.isAutoGenerated == selectedSubtitleAuto && item.name == selectedSubtitleName
                                Button(onClick = { onSelectSubtitle(item.languageTag, item.isAutoGenerated, item.name) }) {
                                    Text("${item.displayLanguageName}${if (selected) " · selected" else ""}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge)
        content()
    }
}

@Composable
private fun VideoOptions(
    tracks: List<StreamVideo>,
    selectedItag: Int?,
    selectedIndex: Int,
    initialFocus: FocusRequester,
    onSelect: (Int) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(tracks) { index, item ->
            val selected = item.itag == selectedItag
            Button(
                modifier = if (index == selectedIndex) Modifier.focusRequester(initialFocus) else Modifier,
                onClick = { onSelect(item.itag) },
            ) {
                Text("${item.resolution} · ${playbackCodecLabel(item.codec).orEmpty()}${if (selected) " · selected" else ""}")
            }
        }
    }
}

@Composable
private fun AudioOptions(
    tracks: List<StreamAudio>,
    selectedItag: Int?,
    selectedTrackId: String?,
    onSelect: (Int, String?) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tracks) { item ->
            val selected = item.itag == selectedItag && (selectedTrackId == null || item.audioTrackId == selectedTrackId)
            val name = item.audioTrackName ?: item.audioLocale ?: "Audio"
            Button(onClick = { onSelect(item.itag, item.audioTrackId) }) {
                Text("$name · ${playbackCodecLabel(item.codec).orEmpty()}${if (selected) " · selected" else ""}")
            }
        }
    }
}
