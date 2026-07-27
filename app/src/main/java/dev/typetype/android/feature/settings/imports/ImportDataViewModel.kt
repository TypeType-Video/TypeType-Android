package dev.typetype.android.feature.settings.imports

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.domain.imports.ImportRepository
import dev.typetype.android.domain.library.LibraryRepository
import dev.typetype.android.domain.subscriptions.SubscriptionsRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ImportDataViewModel @Inject constructor(
    private val documentReader: ImportDocumentReader,
    private val importRepository: ImportRepository,
    private val subscriptionsRepository: SubscriptionsRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ImportDataState())
    val state = _state.asStateFlow()

    fun selectDocument(uri: Uri) {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { documentReader.read(uri) } }
                .onSuccess { document ->
                    _state.value = ImportDataState(selectedDocument = document)
                }
                .onFailure(::showFailure)
        }
    }

    fun restore() {
        val document = _state.value.selectedDocument ?: return
        if (_state.value.isRestoring) return
        _state.update {
            it.copy(
                isRestoring = true,
                summary = null,
                errorKey = null,
                errorRequestId = null,
            )
        }
        viewModelScope.launch {
            importRepository.restorePipePipe(document)
                .onSuccess { summary ->
                    _state.update {
                        it.copy(isRestoring = false, summary = summary)
                    }
                    refreshImportedCollections()
                }
                .onFailure { error ->
                    showFailure(error)
                    _state.update { it.copy(isRestoring = false) }
                }
        }
    }

    fun reset() {
        if (!_state.value.isRestoring) _state.value = ImportDataState()
    }

    private fun refreshImportedCollections() {
        viewModelScope.launch { subscriptionsRepository.refresh() }
        viewModelScope.launch { libraryRepository.refreshHistory() }
        viewModelScope.launch { libraryRepository.refreshPlaylists() }
        viewModelScope.launch { libraryRepository.refreshFavorites() }
        viewModelScope.launch { libraryRepository.refreshWatchLater() }
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
}
