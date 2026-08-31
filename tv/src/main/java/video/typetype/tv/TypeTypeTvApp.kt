package video.typetype.tv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import video.typetype.tv.data.TvViewModel
import video.typetype.tv.data.beginOidc
import video.typetype.tv.data.closeCollection
import video.typetype.tv.data.openChannel
import video.typetype.tv.data.openPlaylist
import video.typetype.tv.data.openSavedPlaylist
import video.typetype.tv.data.openUserPlaylist
import video.typetype.tv.data.playNext
import video.typetype.tv.data.playQueuedVideo
import video.typetype.tv.data.switchVideoTrack
import video.typetype.tv.data.switchAudioTrack
import video.typetype.tv.data.switchSubtitle
import video.typetype.tv.data.openPodcast
import video.typetype.tv.data.openVideo
import video.typetype.tv.data.playVideo
import video.typetype.tv.data.closePodcast
import video.typetype.tv.data.loadMoreChannel
import video.typetype.tv.data.loadMorePlaylist
import video.typetype.tv.data.loadMorePodcast
import video.typetype.tv.data.toggleFavorite
import video.typetype.tv.data.toggleWatchLater
import video.typetype.tv.data.toggleSubscription
import video.typetype.tv.data.toggleSavedPlaylist
import video.typetype.tv.data.togglePlaylistVideo
import video.typetype.tv.data.loadMoreComments
import video.typetype.tv.data.loadCommentReplies
import video.typetype.tv.data.updateSearchQuery
import video.typetype.tv.data.selectSearchContentFilter
import video.typetype.tv.data.selectSearchSortFilter
import video.typetype.tv.data.toggleSearchFilter
import video.typetype.tv.data.search
import video.typetype.tv.data.loadMoreSearch
import video.typetype.tv.data.startAudioOnlyPlayback
import video.typetype.tv.data.createPlaylist
import video.typetype.tv.data.renamePlaylist
import video.typetype.tv.data.deletePlaylist
import video.typetype.tv.data.removePlaylistVideo
import video.typetype.tv.data.movePlaylistVideo
import video.typetype.tv.data.TvPlaylistActions
import video.typetype.tv.data.TvProfileActions
import video.typetype.tv.data.TvSubscriptionGroupActions
import video.typetype.tv.data.updateProfile
import video.typetype.tv.data.updateSettings
import video.typetype.tv.data.setEmojiAvatar
import video.typetype.tv.data.clearAvatar
import video.typetype.tv.data.clearHistory
import video.typetype.tv.data.selectSubscriptionGroup
import video.typetype.tv.data.createSubscriptionGroup
import video.typetype.tv.data.renameSubscriptionGroup
import video.typetype.tv.data.deleteSubscriptionGroup
import video.typetype.tv.data.toggleSubscriptionGroupChannel
import video.typetype.tv.data.startDownload
import video.typetype.tv.data.cancelDownload
import video.typetype.tv.data.retryDownloadArtifact
import video.typetype.tv.data.clearDownload
import video.typetype.tv.ui.TvRoot
import video.typetype.tv.ui.theme.TvAppearance
import video.typetype.tv.ui.theme.TypeTypeTvTheme
import video.typetype.tv.ui.theme.TvAppearanceStore

@Composable
public fun TypeTypeTvApp(
    viewModel: TvViewModel,
    onOpenOidc: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val appearanceStore = remember(context) { TvAppearanceStore(context) }
    val appearance by appearanceStore.appearance.collectAsStateWithLifecycle(TvAppearance())
    val scope = rememberCoroutineScope()
    val playlistActions = remember(viewModel) {
        TvPlaylistActions(
            create = viewModel::createPlaylist,
            rename = viewModel::renamePlaylist,
            delete = viewModel::deletePlaylist,
            removeVideo = viewModel::removePlaylistVideo,
            moveVideo = viewModel::movePlaylistVideo,
        )
    }
    val profileActions = remember(viewModel) {
        TvProfileActions(
            update = viewModel::updateProfile,
            setEmojiAvatar = viewModel::setEmojiAvatar,
            clearAvatar = viewModel::clearAvatar,
        )
    }
    val subscriptionGroupActions = remember(viewModel) {
        TvSubscriptionGroupActions(
            select = viewModel::selectSubscriptionGroup,
            create = viewModel::createSubscriptionGroup,
            rename = viewModel::renameSubscriptionGroup,
            delete = viewModel::deleteSubscriptionGroup,
            toggleChannel = viewModel::toggleSubscriptionGroupChannel,
        )
    }
    TypeTypeTvTheme(appearance) {
        TvRoot(
            state = state,
            appearance = appearance,
            onAppearanceChange = { value -> scope.launch { appearanceStore.save(value) } },
            onSettingsChange = viewModel::updateSettings,
            onNavigate = viewModel::navigate,
            onServiceChange = viewModel::selectService,
            onLogin = viewModel::login,
            onRegister = viewModel::register,
            onOidc = { viewModel.beginOidc(onOpenOidc) },
            onContinueAsGuest = viewModel::continueAsGuest,
            onLogout = viewModel::logout,
            onPlayVideo = viewModel::playVideo,
            onOpenVideo = viewModel::openVideo,
            onOpenChannel = viewModel::openChannel,
            onOpenPlaylist = viewModel::openPlaylist,
            onOpenUserPlaylist = viewModel::openUserPlaylist,
            onOpenSavedPlaylist = viewModel::openSavedPlaylist,
            onOpenPodcast = viewModel::openPodcast,
            onToggleFavorite = viewModel::toggleFavorite,
            onToggleWatchLater = viewModel::toggleWatchLater,
            onToggleSubscription = viewModel::toggleSubscription,
            onToggleSavedPlaylist = viewModel::toggleSavedPlaylist,
            onClearHistory = viewModel::clearHistory,
            onTogglePlaylistVideo = viewModel::togglePlaylistVideo,
            onStartDownload = viewModel::startDownload,
            onCancelDownload = viewModel::cancelDownload,
            onRetryDownloadArtifact = viewModel::retryDownloadArtifact,
            onClearDownload = viewModel::clearDownload,
            playlistActions = playlistActions,
            profileActions = profileActions,
            subscriptionGroupActions = subscriptionGroupActions,
            onLoadMoreChannel = viewModel::loadMoreChannel,
            onLoadMorePlaylist = viewModel::loadMorePlaylist,
            onLoadMorePodcast = viewModel::loadMorePodcast,
            onStartPlayback = viewModel::startPlayback,
            onStartAudioPlayback = viewModel::startAudioOnlyPlayback,
            onSelectVideoTrack = viewModel::selectVideoTrack,
            onSelectAudioTrack = viewModel::selectAudioTrack,
            onSelectSubtitle = viewModel::selectSubtitle,
            onPlayNext = viewModel::playNext,
            onPlayQueueItem = viewModel::playQueuedVideo,
            onSelectVideoTrackDuringPlayback = viewModel::switchVideoTrack,
            onSelectAudioTrackDuringPlayback = viewModel::switchAudioTrack,
            onSelectSubtitleDuringPlayback = viewModel::switchSubtitle,
            onLoadMoreComments = viewModel::loadMoreComments,
            onLoadCommentReplies = viewModel::loadCommentReplies,
            onSearch = viewModel::search,
            onSearchQueryChange = viewModel::updateSearchQuery,
            onSearchContentFilter = viewModel::selectSearchContentFilter,
            onSearchSortFilter = viewModel::selectSearchSortFilter,
            onToggleSearchFilter = viewModel::toggleSearchFilter,
            onLoadMoreSearch = viewModel::loadMoreSearch,
            onClosePlayback = viewModel::closePlayback,
            onCloseDetails = viewModel::closeDetails,
            onCloseCollection = viewModel::closeCollection,
            onClosePodcast = viewModel::closePodcast,
        )
    }
}
