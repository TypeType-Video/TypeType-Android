package dev.typetype.android.feature.settings.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.download.DownloadItem
import dev.typetype.android.domain.download.DownloadRepository
import javax.inject.Inject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class StorageSettingsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
) : ViewModel() {
    private val _events = MutableSharedFlow<StorageEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    val downloads: StateFlow<List<DownloadItem>> = flow {
        coroutineScope {
            launch {
                while (true) {
                    downloadRepository.refreshDownloads()
                    delay(REFRESH_INTERVAL_MS)
                }
            }
            emitAll(downloadRepository.observeDownloads())
        }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun openDownload(requestId: String) {
        runAction { downloadRepository.openDownload(requestId) }
    }

    fun cancelDownload(requestId: String) {
        runAction { downloadRepository.cancelDownload(requestId) }
    }

    fun retryDownload(requestId: String) {
        runAction { downloadRepository.retryDownload(requestId) }
    }

    fun removeDownload(requestId: String) {
        runAction { downloadRepository.removeDownload(requestId) }
    }

    private fun runAction(action: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            action().onFailure { _events.emit(StorageEvent.ActionFailed) }
        }
    }

    private companion object {
        const val REFRESH_INTERVAL_MS = 1_500L
    }
}

enum class StorageEvent {
    ActionFailed,
}
