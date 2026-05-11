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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class StorageSettingsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
) : ViewModel() {
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

    fun openDownload(downloadId: Long) {
        viewModelScope.launch {
            downloadRepository.openDownload(downloadId)
        }
    }

    private companion object {
        const val REFRESH_INTERVAL_MS = 1_500L
    }
}
