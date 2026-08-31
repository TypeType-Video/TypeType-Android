package video.typetype.tv.ui
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import video.typetype.sdk.core.Video
import video.typetype.sdk.core.Channel
import video.typetype.sdk.core.Playlist
import video.typetype.sdk.core.SavedPlaylist
import video.typetype.sdk.core.UserPlaylist
import video.typetype.sdk.core.Podcast
import video.typetype.sdk.core.Comment
import video.typetype.sdk.core.UserSettings
import video.typetype.tv.data.TvAppState
import video.typetype.tv.data.TvDestination
import video.typetype.tv.data.TvAuthStatus
import video.typetype.tv.data.TvPlaylistActions
import video.typetype.tv.data.TvProfileActions
import video.typetype.tv.data.TvSubscriptionGroupActions
import video.typetype.tv.data.availableTvServices
import video.typetype.tv.data.TvDownloadOption
import video.typetype.tv.ui.theme.TvAppearance
import video.typetype.tv.ui.theme.LocalTvAppearance
@Composable
public fun TvRoot(
    state: TvAppState,
    appearance: TvAppearance,
    onAppearanceChange: (TvAppearance) -> Unit,
    onSettingsChange: (UserSettings) -> Unit,
    onNavigate: (TvDestination) -> Unit,
    onServiceChange: (video.typetype.sdk.core.ServiceId) -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit,
    onOidc: () -> Unit,
    onContinueAsGuest: () -> Unit,
    onLogout: () -> Unit,
    onPlayVideo: (Video) -> Unit,
    onOpenVideo: (Video) -> Unit,
    onOpenChannel: (Channel) -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    onOpenUserPlaylist: (UserPlaylist) -> Unit,
    onOpenSavedPlaylist: (SavedPlaylist) -> Unit,
    onOpenPodcast: (Podcast) -> Unit,
    onToggleFavorite: (Video) -> Unit,
    onToggleWatchLater: (Video) -> Unit,
    onToggleSubscription: (Channel) -> Unit,
    onToggleSavedPlaylist: (video.typetype.sdk.core.PublicPlaylist) -> Unit,
    onClearHistory: () -> Unit,
    onTogglePlaylistVideo: (UserPlaylist, Video) -> Unit,
    onStartDownload: (TvDownloadOption) -> Unit,
    onCancelDownload: () -> Unit,
    onRetryDownloadArtifact: () -> Unit,
    onClearDownload: () -> Unit,
    playlistActions: TvPlaylistActions,
    profileActions: TvProfileActions,
    subscriptionGroupActions: TvSubscriptionGroupActions,
    onLoadMoreChannel: () -> Unit,
    onLoadMorePlaylist: () -> Unit,
    onLoadMorePodcast: () -> Unit,
    onStartPlayback: () -> Unit,
    onStartAudioPlayback: () -> Unit,
    onSelectVideoTrack: (Int) -> Unit,
    onSelectAudioTrack: (Int, String?) -> Unit,
    onSelectSubtitle: (String?, Boolean, String?) -> Unit,
    onPlayNext: () -> Unit,
    onPlayQueueItem: (Video) -> Unit,
    onSelectVideoTrackDuringPlayback: (Int, Long) -> Unit,
    onSelectAudioTrackDuringPlayback: (Int, String?, Long) -> Unit,
    onSelectSubtitleDuringPlayback: (String?, Boolean, String?, Long) -> Unit,
    onLoadMoreComments: () -> Unit,
    onLoadCommentReplies: (Comment) -> Unit,
    onSearch: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchContentFilter: (String?) -> Unit,
    onSearchSortFilter: (String?) -> Unit,
    onToggleSearchFilter: (String, String, Boolean) -> Unit,
    onLoadMoreSearch: () -> Unit,
    onClosePlayback: () -> Unit,
    onCloseDetails: () -> Unit,
    onCloseCollection: () -> Unit,
    onClosePodcast: () -> Unit,
) {
    if (state.authStatus == TvAuthStatus.CHECKING || state.authStatus == TvAuthStatus.SIGNED_OUT) {
        LoginScreen(
            state = state,
            onLogin = onLogin,
            onRegister = onRegister,
            onOidc = onOidc,
            onContinueAsGuest = onContinueAsGuest,
        )
        return
    }
    var searchFocusKey by rememberSaveable { mutableStateOf<String?>(null) }
    val stateHolder = rememberSaveableStateHolder()
    val navigationFocus = TvNavigationFocus.remember()
    val mainActive = state.selectedChannel == null && state.selectedPlaylist == null &&
        state.selectedUserPlaylist == null && state.selectedPodcast == null &&
        state.selectedVideo == null && state.playback == null
    val commentsState = state.toCommentsUiState()
    Box(modifier = Modifier.fillMaxSize()) {
        stateHolder.SaveableStateProvider("main-${state.destination}") {
            if (mainActive) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    colors = androidx.tv.material3.SurfaceDefaults.colors(MaterialTheme.colorScheme.background),
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MangaBackdrop(appearance)
                        Box(modifier = Modifier.fillMaxSize()) {
                            when (state.destination) {
                                TvDestination.HOME -> HomeScreen(
                                    state, mainActive, onPlayVideo, onOpenVideo, onOpenChannel,
                                    navigationFocus.homeHero,
                                    subscriptionGroupActions.takeIf {
                                        state.authStatus == TvAuthStatus.AUTHENTICATED
                                    },
                                )
                                TvDestination.SEARCH -> SearchScreen(
                                    state,
                                    mainActive,
                                    searchFocusKey,
                                    { searchFocusKey = it },
                                    onOpenVideo,
                                    onOpenChannel,
                                    onOpenPlaylist,
                                    onSearch,
                                    onSearchQueryChange,
                                    onSearchContentFilter,
                                    onSearchSortFilter,
                                    onToggleSearchFilter,
                                    onLoadMoreSearch,
                                    navigationFocus.searchField,
                                    navigationFocus.tabFor(TvDestination.SEARCH),
                                )
                                TvDestination.LIBRARY -> LibraryScreen(
                                    state,
                                    mainActive,
                                    onOpenVideo,
                                    onOpenUserPlaylist,
                                    onOpenSavedPlaylist,
                                    onClearHistory,
                                    playlistActions.takeIf { state.authStatus == TvAuthStatus.AUTHENTICATED },
                                    subscriptionGroupActions.takeIf {
                                        state.authStatus == TvAuthStatus.AUTHENTICATED
                                    },
                                    navigationFocus.libraryContent, navigationFocus.tabFor(TvDestination.LIBRARY),
                                )
                                TvDestination.SETTINGS -> AppearanceScreen(
                                    profile = state.profile,
                                    settings = state.settings,
                                    appearance = appearance,
                                    services = availableTvServices(state.metadata),
                                    selectedService = state.selectedService,
                                    onServiceChange = onServiceChange,
                                    onSettingsChange = onSettingsChange,
                                    onChange = onAppearanceChange,
                                    profileActions = profileActions.takeIf {
                                        state.authStatus == TvAuthStatus.AUTHENTICATED
                                    },
                                    isActionInProgress = state.isActionInProgress,
                                    onLogout = onLogout, initialFocus = navigationFocus.settingsContent,
                                    topNavigationFocus = navigationFocus.tabFor(TvDestination.SETTINGS),
                                )
                            }
                            TvTopNavigation(
                                selected = state.destination,
                                onNavigate = onNavigate,
                                focusRequesterFor = navigationFocus::tabFor,
                                contentFocusRequester = navigationFocus.contentFor(state.destination),
                            )
                            state.errorMessage?.let { ErrorBanner(it, Modifier.align(Alignment.BottomCenter)) }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) { MangaBackdrop(appearance) }
            }
        }
        val selectedVideo = state.selectedVideo
        val selectedPodcast = state.selectedPodcast
        val selectedChannel = state.selectedChannel
        val selectedPlaylist = state.selectedPlaylist
        val selectedUserPlaylist = state.selectedUserPlaylist
        when {
            selectedVideo == null && selectedPodcast != null -> stateHolder.SaveableStateProvider(
                "podcast-${selectedPodcast.podcast.url}",
            ) { OverlaySurface {
                PodcastEpisodesScreen(
                    page = selectedPodcast,
                    isLoadingMore = state.isLoadingMoreCollection,
                    errorMessage = state.errorMessage,
                    onOpenVideo = onOpenVideo,
                    onLoadMore = onLoadMorePodcast,
                    onBack = onClosePodcast,
                )
            } }
            selectedVideo == null && selectedChannel != null -> stateHolder.SaveableStateProvider(
                "channel-${selectedChannel.url}",
            ) { OverlaySurface {
                ChannelScreen(
                    channel = selectedChannel,
                    podcasts = state.channelPodcasts,
                    playlists = state.channelPlaylists,
                    isSubscribed = state.subscriptions.any { it.channelUrl == selectedChannel.url },
                    isActionInProgress = state.isActionInProgress,
                    isLoadingMore = state.isLoadingMoreCollection,
                    errorMessage = state.errorMessage,
                    onOpenVideo = onOpenVideo,
                    onOpenPodcast = onOpenPodcast,
                    onOpenPlaylist = onOpenPlaylist,
                    onToggleSubscription = { onToggleSubscription(selectedChannel) },
                    onLoadMore = onLoadMoreChannel,
                    onBack = onCloseCollection,
                )
            } }
            selectedVideo == null && selectedPlaylist != null -> stateHolder.SaveableStateProvider(
                "playlist-${selectedPlaylist.playlist.url}",
            ) { OverlaySurface {
                PlaylistScreen(
                    playlist = selectedPlaylist,
                    isAuthenticated = state.authStatus == TvAuthStatus.AUTHENTICATED,
                    isSaved = state.savedPlaylists.any {
                        it.publicPlaylistId == selectedPlaylist.playlist.id ||
                            it.url == selectedPlaylist.playlist.url
                    },
                    isActionInProgress = state.isActionInProgress,
                    isLoadingMore = state.isLoadingMoreCollection,
                    errorMessage = state.errorMessage,
                    onToggleSaved = { onToggleSavedPlaylist(selectedPlaylist) },
                    onOpenVideo = onOpenVideo,
                    onLoadMore = onLoadMorePlaylist,
                    onBack = onCloseCollection,
                )
            } }
            selectedVideo == null && selectedUserPlaylist != null -> stateHolder.SaveableStateProvider(
                "user-playlist-${selectedUserPlaylist.id}",
            ) { OverlaySurface {
                UserPlaylistScreen(
                    playlist = selectedUserPlaylist,
                    isActionInProgress = state.isActionInProgress,
                    errorMessage = state.errorMessage,
                    actions = playlistActions,
                    onOpenVideo = onOpenVideo,
                    onBack = onCloseCollection,
                )
            } }
            selectedVideo != null && state.playback == null -> DetailsScreen(
                video = selectedVideo,
                stream = state.stream,
                isLoading = state.isLoadingDetails,
                errorMessage = state.errorMessage,
                isAuthenticated = state.authStatus == TvAuthStatus.AUTHENTICATED,
                isFavorite = state.favorites.any { it.video.url == selectedVideo.url },
                isInWatchLater = state.watchLater.any { it.video.url == selectedVideo.url },
                isSubscribed = state.stream?.uploaderUrl?.let { uploaderUrl ->
                    state.subscriptions.any { it.channelUrl == uploaderUrl }
                } == true,
                isActionInProgress = state.isActionInProgress,
                playlists = state.playlists,
                downloadJob = state.downloadJob,
                isSavingDownload = state.isSavingDownload,
                downloadMessage = state.downloadMessage,
                downloadError = state.downloadError,
                onPlay = onStartPlayback,
                onPlayAudio = onStartAudioPlayback,
                onOpenRelated = onOpenVideo,
                onToggleFavorite = { onToggleFavorite(selectedVideo) },
                onToggleWatchLater = { onToggleWatchLater(selectedVideo) },
                onTogglePlaylistVideo = onTogglePlaylistVideo,
                onStartDownload = onStartDownload,
                onCancelDownload = onCancelDownload,
                onRetryDownloadArtifact = onRetryDownloadArtifact,
                onClearDownload = onClearDownload,
                onOpenChannel = { state.stream?.asChannel()?.let(onOpenChannel) },
                onToggleSubscription = { state.stream?.asChannel()?.let(onToggleSubscription) },
                commentsState = commentsState,
                onLoadMoreComments = onLoadMoreComments,
                onLoadCommentReplies = onLoadCommentReplies,
                selectedVideoItag = state.selectedVideoItag,
                supportedVideoItags = state.supportedVideoItags,
                selectedAudioItag = state.selectedAudioItag,
                selectedAudioTrackId = state.selectedAudioTrackId,
                selectedSubtitleLanguage = state.selectedSubtitleLanguage,
                selectedSubtitleAuto = state.selectedSubtitleAuto,
                selectedSubtitleName = state.selectedSubtitleName,
                onSelectVideoTrack = onSelectVideoTrack,
                onSelectAudioTrack = onSelectAudioTrack,
                onSelectSubtitle = onSelectSubtitle,
                onBack = onCloseDetails,
            )
        }
        ActivePlayer(
            state = state,
            commentsState = commentsState,
            onClose = onClosePlayback,
            onPlayNext = onPlayNext,
            onPlayQueueItem = onPlayQueueItem,
            onSelectVideoTrack = onSelectVideoTrackDuringPlayback,
            onSelectAudioTrack = onSelectAudioTrackDuringPlayback,
            onSelectSubtitle = onSelectSubtitleDuringPlayback,
            onLoadMoreComments = onLoadMoreComments,
            onLoadCommentReplies = onLoadCommentReplies,
        )
    }
}
