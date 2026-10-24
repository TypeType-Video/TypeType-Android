package dev.typetype.android.feature.settings.imports

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.domain.actions.VideoActionsRepository
import dev.typetype.android.domain.imports.ImportDocument
import dev.typetype.android.domain.imports.ImportRepository
import dev.typetype.android.domain.imports.TypeTypeBackupCategory
import dev.typetype.android.domain.library.LibraryRepository
import dev.typetype.android.domain.publicplaylist.SavedPublicPlaylistRepository
import dev.typetype.android.domain.subscriptions.SubscriptionsRepository
import dev.typetype.android.domain.usersettings.UserSettingsRepository
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
    private val savedPublicPlaylistRepository: SavedPublicPlaylistRepository,
    private val videoActionsRepository: VideoActionsRepository,
    private val userSettingsRepository: UserSettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ImportDataState())
    val state = _state.asStateFlow()

    fun toggleTypeTypeCategory(category: TypeTypeBackupCategory) {
        if (_state.value.isExportingTypeType || _state.value.isRestoringTypeType) return
        _state.update { state ->
            val categories = state.selectedCategories.toMutableSet()
            if (!categories.add(category)) categories.remove(category)
            state.copy(selectedCategories = categories, typeTypeExportComplete = false)
        }
    }

    fun exportTypeType(destination: Uri) {
        val categories = _state.value.selectedCategories
        if (!_state.value.canExportTypeType) return
        _state.update {
            it.copy(
                isExportingTypeType = true,
                typeTypeExportComplete = false,
                errorKey = null,
                errorRequestId = null,
            )
        }
        viewModelScope.launch {
            importRepository.exportTypeType(categories, destination.toString())
                .onSuccess {
                    _state.update {
                        it.copy(isExportingTypeType = false, typeTypeExportComplete = true)
                    }
                }
                .onFailure { error ->
                    showFailure(error)
                    _state.update { it.copy(isExportingTypeType = false) }
                }
        }
    }

    fun selectTypeTypeDocument(uri: Uri) {
        readDocument(uri, "typetype-backup.json") { document ->
            copy(
                selectedTypeTypeDocument = document,
                typeTypeSummary = null,
                errorKey = null,
                errorRequestId = null,
            )
        }
    }

    fun dismissTypeTypeRestore() {
        if (!_state.value.isRestoringTypeType) {
            _state.update { it.copy(selectedTypeTypeDocument = null) }
        }
    }

    fun restoreTypeType() {
        val document = _state.value.selectedTypeTypeDocument ?: return
        if (_state.value.isRestoringTypeType) return
        _state.update {
            it.copy(
                isRestoringTypeType = true,
                typeTypeSummary = null,
                errorKey = null,
                errorRequestId = null,
            )
        }
        viewModelScope.launch {
            importRepository.restoreTypeType(document)
                .onSuccess { summary ->
                    _state.update {
                        it.copy(
                            isRestoringTypeType = false,
                            selectedTypeTypeDocument = null,
                            typeTypeSummary = summary,
                        )
                    }
                    refreshImportedCollections()
                }
                .onFailure { error ->
                    showFailure(error)
                    _state.update { it.copy(isRestoringTypeType = false) }
                }
        }
    }

    fun selectPipePipeDocument(uri: Uri) {
        readDocument(uri, "backup.zip") { document ->
            copy(
                selectedPipePipeDocument = document,
                pipePipeSummary = null,
                errorKey = null,
                errorRequestId = null,
            )
        }
    }

    private fun readDocument(
        uri: Uri,
        fallbackName: String,
        update: ImportDataState.(ImportDocument) -> ImportDataState,
    ) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { documentReader.read(uri, fallbackName) }
            }
                .onSuccess { document ->
                    _state.update { it.update(document) }
                }
                .onFailure(::showFailure)
        }
    }

    fun restorePipePipe() {
        val document = _state.value.selectedPipePipeDocument ?: return
        if (_state.value.isRestoringPipePipe) return
        _state.update {
            it.copy(
                isRestoringPipePipe = true,
                pipePipeSummary = null,
                errorKey = null,
                errorRequestId = null,
            )
        }
        viewModelScope.launch {
            importRepository.restorePipePipe(document)
                .onSuccess { summary ->
                    _state.update {
                        it.copy(
                            isRestoringPipePipe = false,
                            selectedPipePipeDocument = null,
                            pipePipeSummary = summary,
                        )
                    }
                    refreshImportedCollections()
                }
                .onFailure { error ->
                    showFailure(error)
                    _state.update { it.copy(isRestoringPipePipe = false) }
                }
        }
    }

    fun resetTypeTypeResult() {
        if (!_state.value.isRestoringTypeType) {
            _state.update { it.copy(typeTypeSummary = null, typeTypeExportComplete = false) }
        }
    }

    fun resetPipePipeResult() {
        if (!_state.value.isRestoringPipePipe) {
            _state.update { it.copy(pipePipeSummary = null, selectedPipePipeDocument = null) }
        }
    }

    private fun refreshImportedCollections() {
        viewModelScope.launch { subscriptionsRepository.refresh() }
        viewModelScope.launch { libraryRepository.refreshHistory() }
        viewModelScope.launch { libraryRepository.refreshPlaylists() }
        viewModelScope.launch { libraryRepository.refreshFavorites() }
        viewModelScope.launch { libraryRepository.refreshWatchLater() }
        viewModelScope.launch { savedPublicPlaylistRepository.refresh() }
        viewModelScope.launch { videoActionsRepository.refreshBlocked() }
        viewModelScope.launch { userSettingsRepository.refresh() }
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
