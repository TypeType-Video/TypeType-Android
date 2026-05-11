package dev.typetype.android.feature.library

import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.LocalAppSnackbarHost
import dev.typetype.android.core.ui.components.PlaylistVideoActionsSheet
import dev.typetype.android.core.ui.components.PlaylistVideoCard
import dev.typetype.android.core.ui.share.LocalServerBaseUrl
import dev.typetype.android.core.ui.share.buildShareUrl
import dev.typetype.android.domain.library.PlaylistVideo
import dev.typetype.android.feature.library.components.rememberVideoMetas
import dev.typetype.android.feature.menu.VideoMenuEvent
import dev.typetype.android.feature.menu.VideoMenuHandlerViewModel

@Composable
fun rememberLibraryMenuHandler(): Pair<VideoMenuHandlerViewModel, Set<String>> {
    val menuVm: VideoMenuHandlerViewModel = hiltViewModel()
    val watchedUrls by menuVm.watchedUrls.collectAsStateWithLifecycle()
    val snackbarHost = LocalAppSnackbarHost.current
    LaunchedEffect(menuVm, snackbarHost) {
        if (snackbarHost == null) return@LaunchedEffect
        menuVm.events.collect { event ->
            when (event) {
                is VideoMenuEvent.Snackbar -> snackbarHost.showSnackbar(
                    event.message,
                    duration = SnackbarDuration.Short,
                )
            }
        }
    }
    return menuVm to watchedUrls
}

@Composable
fun PlaylistContextTab(
    items: List<PlaylistVideo>,
    filter: String,
    emptyDefault: String,
    watchedUrls: Set<String>,
    onPlayVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    buildRemoveLabel: @Composable (PlaylistVideo) -> String,
    onRemove: (PlaylistVideo) -> Unit,
    onToggleWatched: (PlaylistVideo, Boolean) -> Unit,
    onBlockVideo: (PlaylistVideo) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyTab(emptyMessageFor(filter, emptyDefault))
        return
    }
    val context = LocalContext.current
    val shareChooserTitle = stringResource(R.string.video_menu_share_chooser)
    val serverBaseUrl = LocalServerBaseUrl.current
    var pendingMenu by remember { mutableStateOf<PlaylistVideo?>(null) }
    val urlsMissingInfo = items
        .filter { it.channelAvatarUrl.isBlank() || it.channelName.isBlank() }
        .map { it.url }
    val metas = rememberVideoMetas(urlsMissingInfo)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(items, key = { it.id }) { video ->
            PlaylistVideoCard(
                video = video,
                onClick = { onPlayVideo(video.url) },
                onLongPress = { pendingMenu = video },
                isWatched = video.url in watchedUrls,
                meta = metas[video.url],
                onChannelClick = onOpenChannel,
            )
        }
    }

    pendingMenu?.let { video ->
        PlaylistVideoActionsSheet(
            removeLabel = buildRemoveLabel(video),
            isWatched = video.url in watchedUrls,
            onRemoveFromList = { onRemove(video) },
            onToggleWatched = { onToggleWatched(video, video.url in watchedUrls) },
            onShare = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, buildShareUrl(serverBaseUrl, video.url))
                }
                context.startActivity(Intent.createChooser(intent, shareChooserTitle))
            },
            onBlockVideo = { onBlockVideo(video) },
            onDismiss = { pendingMenu = null },
        )
    }
}
