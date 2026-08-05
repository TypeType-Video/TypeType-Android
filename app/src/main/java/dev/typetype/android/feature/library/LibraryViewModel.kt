package dev.typetype.android.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.LibrarySortMode
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.domain.library.FavoriteItem
import dev.typetype.android.domain.library.HistoryItem
import dev.typetype.android.domain.library.HistoryDateRange
import dev.typetype.android.domain.library.HistoryOrder
import dev.typetype.android.domain.library.HistoryQuery
import dev.typetype.android.domain.library.LibraryCollection
import dev.typetype.android.domain.library.LibraryRepository
import dev.typetype.android.domain.library.Playlist
import dev.typetype.android.domain.library.PlaylistVideo
import dev.typetype.android.domain.library.WatchLaterItem
import dev.typetype.android.domain.publicplaylist.SavedPublicPlaylist
import dev.typetype.android.domain.publicplaylist.SavedPublicPlaylistRepository
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository,
    private val savedPlaylistRepository: SavedPublicPlaylistRepository,
    private val errorMapper: UserErrorMapper,
) : ViewModel() {
    private val mutableState = MutableStateFlow(LibraryState(isLoading = true))
    private val historyQuery = MutableStateFlow(HistoryQuery())
    val historyPagingData: Flow<PagingData<HistoryItem>> = historyQuery
        .flatMapLatest(repository::observeHistory)
        .cachedIn(viewModelScope)
    private val content = combine(
        repository.observeHistoryCount(),
        repository.observeFavorites(),
        repository.observeWatchLater(),
        repository.observePlaylists(),
        savedPlaylistRepository.observe(),
    ) { historyCount, favorites, watchLater, playlists, savedPlaylists ->
        LibraryContent(historyCount, favorites, watchLater, playlists, savedPlaylists)
    }

    val state = combine(
        mutableState,
        content,
        repository.observeSyncState(),
        savedPlaylistRepository.observeCanModify(),
    ) { base, content, syncStates, canSavePublicPlaylists ->
        val sync = syncStates[base.selectedTab.syncCollection()]
        val storedFailure = when {
            sync?.failedWriteCount?.let { it > 0 } == true -> errorMapper.message(
                failureCode = sync.writeFailureCode,
                statusCode = sync.writeFailureStatusCode,
                fallbackRes = R.string.library_changes_sync_failed,
            )
            sync?.lastFailureAtMillis != null -> errorMapper.message(
                failureCode = sync.failureCode,
                statusCode = sync.failureStatusCode,
                fallbackRes = R.string.library_refresh_failed,
            )
            else -> null
        }
        base.copy(
            historyItemCount = content.historyCount,
            favorites = content.favorites.map(FavoriteItem::asPlaylistVideo),
            watchLater = content.watchLater.map(WatchLaterItem::asPlaylistVideo),
            playlists = content.playlists,
            savedPlaylists = content.savedPlaylists,
            canSavePublicPlaylists = canSavePublicPlaylists,
            errorMessage = if (base.isLoading) null else base.errorMessage ?: storedFailure,
            lastSuccessfulSyncAtMillis = sync?.lastSuccessAtMillis,
            syncRequestId = if (base.isLoading) {
                null
            } else {
                base.errorRequestId ?: sync?.writeRequestId ?: sync?.requestId
            },
            pendingWriteCount = sync?.pendingWriteCount ?: 0,
            failedWriteCount = sync?.failedWriteCount ?: 0,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = mutableState.value,
    )

    private var refreshJob: Job? = null
    private var historyQueryRefreshJob: Job? = null

    init {
        refresh()
    }

    fun onAction(action: LibraryAction) {
        when (action) {
            is LibraryAction.OnTabSelect -> mutableState.update {
                it.copy(selectedTab = action.tab, errorMessage = null, errorRequestId = null)
            }
            is LibraryAction.OnCreatePlaylist -> createPlaylist(action.name)
            is LibraryAction.OnRenamePlaylist -> renamePlaylist(action.playlistId, action.name)
            is LibraryAction.OnDeletePlaylist -> deletePlaylist(action.playlistId)
            is LibraryAction.OnRemoveSavedPlaylist -> removeSavedPlaylist(action.savedPlaylistId)
            LibraryAction.OnRefresh -> refresh()
            LibraryAction.OnRetry -> retry()
            LibraryAction.OnLoadMoreHistory -> loadMoreHistory()
        }
    }

    fun updateHistoryQuery(
        search: String,
        sort: LibrarySortMode,
        dateRange: HistoryDateRange?,
    ) {
        val order = when (sort) {
            LibrarySortMode.OldestFirst -> HistoryOrder.Oldest
            LibrarySortMode.TitleAZ -> HistoryOrder.TitleAscending
            LibrarySortMode.TitleZA -> HistoryOrder.TitleDescending
            else -> HistoryOrder.Recent
        }
        val query = HistoryQuery(
            search = search,
            order = order,
            fromMillis = dateRange?.fromMillis,
            toMillis = dateRange?.toMillis,
        )
        val previous = historyQuery.value
        historyQuery.value = query
        if (previous.remoteFilterKey() == query.remoteFilterKey()) return
        historyQueryRefreshJob?.cancel()
        historyQueryRefreshJob = viewModelScope.launch {
            delay(HISTORY_QUERY_DEBOUNCE_MS)
            refreshHistoryQuery(query)
        }
    }

    private suspend fun refreshHistoryQuery(query: HistoryQuery) {
        mutableState.update {
            it.copy(
                isLoading = true,
                isLoadingMoreHistory = false,
                historyHasMore = !query.hasRemoteFilter,
                errorMessage = null,
                errorRequestId = null,
            )
        }
        val failure = repository.refreshHistory(query).exceptionOrNull()
        val details = failure?.let { errorMapper.details(it, R.string.library_refresh_failed) }
        mutableState.update {
            it.copy(
                isLoading = false,
                errorMessage = details?.message,
                errorRequestId = details?.requestId,
            )
        }
    }

    private fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            mutableState.update {
                it.copy(
                    isLoading = true,
                    isLoadingMoreHistory = false,
                    historyHasMore = true,
                    errorMessage = null,
                    errorRequestId = null,
                )
            }
            val results = mapOf(
                LibraryCollection.History to async { repository.refreshHistory(historyQuery.value) },
                LibraryCollection.Favorites to async { repository.refreshFavorites() },
                LibraryCollection.WatchLater to async { repository.refreshWatchLater() },
                LibraryCollection.Playlists to async { repository.refreshPlaylists() },
                LibraryCollection.SavedPlaylists to async { savedPlaylistRepository.refresh() },
            )
            results.values.awaitAll()
            val selectedFailure = results[mutableState.value.selectedTab.syncCollection()]
                ?.await()?.exceptionOrNull()
            val details = selectedFailure?.let {
                errorMapper.details(it, R.string.library_refresh_failed)
            }
            mutableState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = details?.message,
                    errorRequestId = details?.requestId,
                )
            }
        }
    }

    private fun loadMoreHistory() {
        val current = mutableState.value
        if (current.isLoading || current.isLoadingMoreHistory || !current.historyHasMore) return
        viewModelScope.launch {
            mutableState.update { it.copy(isLoadingMoreHistory = true) }
            repository.loadMoreHistory(historyQuery.value).fold(
                onSuccess = { hasMore ->
                    mutableState.update {
                        it.copy(isLoadingMoreHistory = false, historyHasMore = hasMore)
                    }
                },
                onFailure = { failure ->
                    val details = errorMapper.details(failure, R.string.library_refresh_failed)
                    mutableState.update {
                        it.copy(
                            isLoadingMoreHistory = false,
                            errorMessage = details.message,
                            errorRequestId = details.requestId,
                        )
                    }
                },
            )
        }
    }

    private fun retry() {
        val collection = mutableState.value.selectedTab.syncCollection()
        if (state.value.failedWriteCount == 0) {
            refresh()
            return
        }
        viewModelScope.launch {
            repository.retryPendingWrites(collection).onFailure { failure ->
                val details = errorMapper.details(failure, R.string.library_changes_sync_failed)
                mutableState.update {
                    it.copy(errorMessage = details.message, errorRequestId = details.requestId)
                }
            }
        }
    }

    private fun createPlaylist(name: String) {
        val cleaned = name.trim()
        if (cleaned.isEmpty()) return
        runPlaylistMutation { repository.createPlaylist(cleaned) }
    }

    private fun renamePlaylist(playlistId: String, name: String) {
        val cleaned = name.trim()
        if (cleaned.isEmpty()) return
        runPlaylistMutation { repository.renamePlaylist(playlistId, cleaned) }
    }

    private fun deletePlaylist(playlistId: String) {
        runPlaylistMutation { repository.deletePlaylist(playlistId) }
    }

    private fun removeSavedPlaylist(savedPlaylistId: String) {
        if (mutableState.value.isSavedPlaylistMutationInFlight) return
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    isSavedPlaylistMutationInFlight = true,
                    errorMessage = null,
                    errorRequestId = null,
                )
            }
            savedPlaylistRepository.remove(savedPlaylistId).fold(
                onSuccess = {
                    mutableState.update { it.copy(isSavedPlaylistMutationInFlight = false) }
                },
                onFailure = { failure ->
                    val details = errorMapper.details(failure, R.string.library_saved_playlist_remove_failed)
                    mutableState.update {
                        it.copy(
                            isSavedPlaylistMutationInFlight = false,
                            errorMessage = details.message,
                            errorRequestId = details.requestId,
                        )
                    }
                },
            )
        }
    }

    private fun runPlaylistMutation(operation: suspend () -> Result<*>) {
        if (mutableState.value.isPlaylistMutationInFlight) return
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    isPlaylistMutationInFlight = true,
                    errorMessage = null,
                    errorRequestId = null,
                )
            }
            operation().fold(
                onSuccess = {
                    mutableState.update { it.copy(isPlaylistMutationInFlight = false) }
                },
                onFailure = { failure ->
                    val details = errorMapper.details(failure, R.string.playlist_manage_failed)
                    mutableState.update {
                        it.copy(
                            isPlaylistMutationInFlight = false,
                            errorMessage = details.message,
                            errorRequestId = details.requestId,
                        )
                    }
                },
            )
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val HISTORY_QUERY_DEBOUNCE_MS = 300L
    }
}
