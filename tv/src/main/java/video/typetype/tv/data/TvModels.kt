package video.typetype.tv.data

import video.typetype.sdk.core.PlaybackSession
import video.typetype.sdk.core.SearchPage
import video.typetype.sdk.core.SearchFilters
import video.typetype.sdk.core.StreamDetails
import video.typetype.sdk.core.Video
import video.typetype.sdk.core.InstanceMetadata
import video.typetype.sdk.core.FavoriteItem
import video.typetype.sdk.core.HistoryItem
import video.typetype.sdk.core.UserPlaylist
import video.typetype.sdk.core.WatchLaterItem
import video.typetype.sdk.core.UserProfile
import video.typetype.sdk.core.Channel
import video.typetype.sdk.core.Comment
import video.typetype.sdk.core.PodcastPage
import video.typetype.sdk.core.PodcastEpisodesPage
import video.typetype.sdk.core.PublicPlaylist
import video.typetype.sdk.core.PlaylistPage
import video.typetype.sdk.core.SavedPlaylist
import video.typetype.sdk.core.Subscription
import video.typetype.sdk.core.SubscriptionGroup
import video.typetype.sdk.core.ServiceId
import video.typetype.sdk.core.AudioOnlyStream
import video.typetype.sdk.core.UserSettings
import video.typetype.sdk.core.DownloadJob

public enum class TvDestination {
    HOME,
    SEARCH,
    LIBRARY,
    SETTINGS,
}

public enum class TvAuthStatus {
    CHECKING,
    SIGNED_OUT,
    AUTHENTICATED,
    GUEST,
}

public data class TvAppState(
    val authStatus: TvAuthStatus = TvAuthStatus.CHECKING,
    val profile: UserProfile? = null,
    val settings: UserSettings = UserSettings(),
    val destination: TvDestination = TvDestination.HOME,
    val metadata: InstanceMetadata? = null,
    val home: List<Video> = emptyList(),
    val trending: List<Video> = emptyList(),
    val history: List<HistoryItem> = emptyList(),
    val watchLater: List<WatchLaterItem> = emptyList(),
    val favorites: List<FavoriteItem> = emptyList(),
    val playlists: List<UserPlaylist> = emptyList(),
    val savedPlaylists: List<SavedPlaylist> = emptyList(),
    val subscriptions: List<Subscription> = emptyList(),
    val subscriptionGroups: List<SubscriptionGroup> = emptyList(),
    val selectedSubscriptionGroupId: String? = null,
    val subscriptionFeed: List<Video> = emptyList(),
    val shorts: List<Video> = emptyList(),
    val isLoadingLibrary: Boolean = false,
    val searchPage: SearchPage? = null,
    val searchQuery: String = "",
    val searchSuggestions: List<String> = emptyList(),
    val searchFilters: SearchFilters? = null,
    val selectedSearchContentFilter: String? = null,
    val selectedSearchSortFilter: String? = null,
    val selectedSearchFilters: Map<String, List<String>> = emptyMap(),
    val isLoadingSearch: Boolean = false,
    val isLoadingMoreSearch: Boolean = false,
    val selectedVideo: Video? = null,
    val selectedService: ServiceId = ServiceId.YOUTUBE,
    val selectedChannel: Channel? = null,
    val channelPlaylists: PlaylistPage? = null,
    val selectedPlaylist: PublicPlaylist? = null,
    val selectedUserPlaylist: UserPlaylist? = null,
    val channelPodcasts: PodcastPage? = null,
    val selectedPodcast: PodcastEpisodesPage? = null,
    val stream: StreamDetails? = null,
    val comments: List<Comment> = emptyList(),
    val commentsNextPage: String? = null,
    val commentsDisabled: Boolean = false,
    val isLoadingComments: Boolean = false,
    val isLoadingMoreComments: Boolean = false,
    val commentReplies: Map<String, List<Comment>> = emptyMap(),
    val loadingCommentReplies: Set<String> = emptySet(),
    val supportedVideoItags: Set<Int> = emptySet(),
    val playback: PlaybackSession? = null,
    val audioOnlyStream: AudioOnlyStream? = null,
    val isAdvancingPlayback: Boolean = false,
    val isActionInProgress: Boolean = false,
    val isLoadingMoreCollection: Boolean = false,
    val selectedVideoItag: Int? = null,
    val selectedAudioItag: Int? = null,
    val selectedAudioTrackId: String? = null,
    val selectedSubtitleLanguage: String? = null,
    val selectedSubtitleAuto: Boolean = false,
    val selectedSubtitleName: String? = null,
    val downloadJob: DownloadJob? = null,
    val isSavingDownload: Boolean = false,
    val downloadMessage: String? = null,
    val downloadError: String? = null,
    val isLoading: Boolean = true,
    val isLoadingDetails: Boolean = false,
    val errorMessage: String? = null,
)

internal fun availableTvServices(metadata: video.typetype.sdk.core.InstanceMetadata?): List<ServiceId> {
    val values = metadata?.supportedServices.orEmpty().map(::ServiceId).distinct()
    return values.ifEmpty { listOf(ServiceId.YOUTUBE) }
}

internal fun ServiceId.displayName(): String = when (value) {
    0 -> "YouTube"
    5 -> "BiliBili"
    6 -> "NicoNico"
    3 -> "SoundCloud"
    4 -> "Media CCC"
    else -> "Service $value"
}

public fun interface TvNavigationAction {
    public fun invoke(destination: TvDestination)
}

public data class TvPlaylistActions(
    val create: (String) -> Unit,
    val rename: (UserPlaylist, String) -> Unit,
    val delete: (UserPlaylist) -> Unit,
    val removeVideo: (UserPlaylist, Video) -> Unit,
    val moveVideo: (UserPlaylist, Video, Int) -> Unit,
)

public data class TvProfileActions(
    val update: (String?, String?) -> Unit,
    val setEmojiAvatar: (String) -> Unit,
    val clearAvatar: () -> Unit,
)

public data class TvSubscriptionGroupActions(
    val select: (String?) -> Unit,
    val create: (String) -> Unit,
    val rename: (SubscriptionGroup, String) -> Unit,
    val delete: (SubscriptionGroup) -> Unit,
    val toggleChannel: (SubscriptionGroup, Subscription) -> Unit,
)
