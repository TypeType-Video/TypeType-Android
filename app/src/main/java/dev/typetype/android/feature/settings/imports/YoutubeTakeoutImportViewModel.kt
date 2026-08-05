package dev.typetype.android.feature.settings.imports

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.domain.imports.YoutubeTakeoutImportItem
import dev.typetype.android.domain.imports.YoutubeTakeoutImportRepository
import dev.typetype.android.domain.library.LibraryRepository
import dev.typetype.android.domain.subscriptions.SubscriptionsRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@HiltViewModel
class YoutubeTakeoutImportViewModel @Inject constructor(
    private val documentReader: ImportDocumentReader,
    private val repository: YoutubeTakeoutImportRepository,
    private val subscriptionsRepository: SubscriptionsRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {
    private val refreshMutex = Mutex()
    private val _state = MutableStateFlow(YoutubeTakeoutImportState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeImports().collectLatest { items ->
                _state.update { it.copy(items = items) }
                refreshCompletedImports(items)
            }
        }
    }

    fun selectDocuments(uris: List<Uri>) {
        if (uris.isEmpty() || _state.value.isReadingDocuments) return
        _state.update { it.copy(isReadingDocuments = true, errorKey = null, errorRequestId = null) }
        viewModelScope.launch {
            try {
                val documents = withContext(Dispatchers.IO) {
                    uris.mapIndexed { index, uri ->
                        documentReader.read(uri, "youtube-takeout-${index + 1}.zip")
                    }
                }
                repository.enqueue(documents).getOrThrow()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                showFailure(failure)
            } finally {
                _state.update { it.copy(isReadingDocuments = false) }
            }
        }
    }

    fun retry(requestId: String) = runOperation { repository.retry(requestId) }

    fun cancel(requestId: String) = runOperation { repository.cancel(requestId) }

    fun remove(requestId: String) = runOperation { repository.remove(requestId) }

    fun retryCollectionRefresh() {
        viewModelScope.launch { refreshCompletedImports(_state.value.items) }
    }

    fun dismissError() {
        _state.update { it.copy(errorKey = null, errorRequestId = null) }
    }

    private fun runOperation(operation: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _state.update { it.copy(errorKey = null, errorRequestId = null) }
            operation().onFailure(::showFailure)
        }
    }

    private suspend fun refreshCompletedImports(items: List<YoutubeTakeoutImportItem>) {
        val pending = items.filter(YoutubeTakeoutImportItem::needsCollectionRefresh)
        if (pending.isEmpty()) return
        refreshMutex.withLock {
            val results = coroutineScope {
                listOf(
                    async { subscriptionsRepository.refresh() },
                    async { libraryRepository.refreshHistory() },
                    async { libraryRepository.refreshPlaylists() },
                    async { libraryRepository.refreshFavorites() },
                    async { libraryRepository.refreshWatchLater() },
                ).awaitAll()
            }
            val refreshFailure = results.firstOrNull { it.isFailure }?.exceptionOrNull()
            if (refreshFailure != null) {
                showFailure(ImportCollectionRefreshFailure(refreshFailure))
                return@withLock
            }
            pending.forEach { item ->
                repository.acknowledgeCollectionRefresh(item.requestId).onFailure {
                    showFailure(ImportCollectionRefreshFailure(it))
                    return@withLock
                }
            }
        }
    }

    private fun showFailure(error: Throwable) {
        val coded = error as? CodedFailure
        _state.update {
            it.copy(
                errorKey = coded?.failureCode ?: error.message.orEmpty(),
                errorRequestId = coded?.requestId,
            )
        }
    }

    private class ImportCollectionRefreshFailure(cause: Throwable) :
        Exception("YOUTUBE_IMPORT_REFRESH_FAILED", cause)
}
