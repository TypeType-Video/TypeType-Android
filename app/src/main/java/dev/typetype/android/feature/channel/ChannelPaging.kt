package dev.typetype.android.feature.channel

import dev.typetype.android.domain.channel.Channel
import dev.typetype.android.domain.channel.ChannelPage
import dev.typetype.android.domain.channel.ChannelPlaylistsPage

internal fun Channel.appendPage(page: Channel): Channel =
    copy(videos = (videos + page.videos).distinctBy { it.url })

internal fun nextChannelCursor(requested: String, received: String?): String? =
    received?.takeUnless { it == requested }

internal fun ChannelState.startPageLoad(): ChannelState = copy(
    isLoadingMore = true,
    loadMoreError = false,
    errorMessage = null,
    errorRequestId = null,
)

internal fun ChannelState.appendPage(page: ChannelPage, requestedCursor: String): ChannelState = copy(
    channel = requireNotNull(channel).appendPage(page.channel),
    nextPage = nextChannelCursor(requestedCursor, page.nextPage),
    isLoadingMore = false,
)

internal fun ChannelState.failPageLoad(message: String, requestId: String?): ChannelState = copy(
    isLoadingMore = false,
    loadMoreError = true,
    errorMessage = message,
    errorRequestId = requestId,
)

internal fun ChannelState.finishChannelLoad(page: ChannelPage): ChannelState = copy(
    isLoading = false,
    channel = page.channel,
    nextPage = page.nextPage,
    loadMoreError = false,
    errorMessage = null,
    errorRequestId = null,
)

internal fun ChannelState.finishPlaylistsLoad(page: ChannelPlaylistsPage): ChannelState = copy(
    playlists = page.playlists.distinctBy { it.url },
    playlistsLoaded = true,
    playlistsNextPage = page.nextPage,
    playlistsLoading = false,
    playlistsLoadMoreError = false,
    playlistsErrorMessage = null,
    playlistsErrorRequestId = null,
)

internal fun ChannelState.appendPlaylistsPage(
    page: ChannelPlaylistsPage,
    requestedCursor: String,
): ChannelState = copy(
    playlists = (playlists + page.playlists).distinctBy { it.url },
    playlistsNextPage = nextChannelCursor(requestedCursor, page.nextPage),
    playlistsLoadingMore = false,
    playlistsLoadMoreError = false,
    playlistsErrorMessage = null,
    playlistsErrorRequestId = null,
)
