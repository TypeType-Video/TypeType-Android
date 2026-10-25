package dev.typetype.android.feature.player

import dev.typetype.android.domain.library.LibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

internal class PlayerLibraryStatusObserver(
    private val repository: LibraryRepository,
    private val scope: CoroutineScope,
    private val currentUrl: () -> String?,
    private val onFavoriteChanged: (Boolean) -> Unit,
    private val onWatchLaterChanged: (Boolean) -> Unit,
) {
    private var favoriteJob: Job? = null
    private var watchLaterJob: Job? = null

    fun observe(url: String) {
        clear()
        favoriteJob = scope.launch {
            repository.observeIsFavorite(url).distinctUntilChanged().collect { isFavorite ->
                if (currentUrl() == url) onFavoriteChanged(isFavorite)
            }
        }
        watchLaterJob = scope.launch {
            repository.observeIsInWatchLater(url).distinctUntilChanged().collect { inWatchLater ->
                if (currentUrl() == url) onWatchLaterChanged(inWatchLater)
            }
        }
    }

    fun clear() {
        favoriteJob?.cancel()
        watchLaterJob?.cancel()
        favoriteJob = null
        watchLaterJob = null
    }
}
