package dev.typetype.android.feature.settings.imports

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.domain.imports.ImportDocument
import dev.typetype.android.domain.imports.PortabilityDuplicatePolicy
import dev.typetype.android.domain.imports.PortabilityJob
import dev.typetype.android.domain.imports.PortabilityFormat
import dev.typetype.android.domain.imports.PortabilityJobState
import dev.typetype.android.domain.imports.PortabilityRepository
import dev.typetype.android.domain.imports.TypeTypeBackupCategory
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PortabilityViewModel @Inject constructor(
    private val portabilityRepository: PortabilityRepository,
    private val documentReader: ImportDocumentReader,
) : ViewModel() {
    private val _state = MutableStateFlow(PortabilityUiState())
    val state = _state.asStateFlow()

    init {
        loadFormats()
    }

    fun loadFormats() {
        viewModelScope.launch {
            _state.update { it.clearFailure().copy(isLoadingFormats = true) }
            portabilityRepository.formats()
                .onSuccess { formats ->
                    val mode = _state.value.mode
                    val supported = formats.filter { format ->
                        format.capabilities.any { capability -> mode.direction in capability.directions }
                    }
                    val selected = supported.firstOrNull()
                    _state.update {
                        it.copy(
                            formats = supported,
                            selectedFormat = selected,
                            selectedCategories = emptySet(),
                            isLoadingFormats = false,
                        )
                    }
                }
                .onFailure(::showFailure)
        }
    }

    fun selectMode(mode: PortabilityScreenMode) {
        val formats = _state.value.formats.filter { format ->
            format.capabilities.any { capability -> mode.direction in capability.directions }
        }
        val selected = formats.firstOrNull()
        _state.update {
            it.copy(
                mode = mode,
                formats = formats,
                selectedFormat = selected,
                selectedCategories = emptySet(),
                job = null,
                failureCode = null,
                failureRequestId = null,
            )
        }
    }

    fun selectFormat(format: PortabilityFormat) {
        _state.update {
            it.copy(
                selectedFormat = format,
                selectedCategories = emptySet(),
                job = null,
            )
        }
    }

    fun toggleCategory(category: TypeTypeBackupCategory) {
        _state.update { current ->
            val values = current.selectedCategories.toMutableSet()
            if (!values.add(category)) values.remove(category)
            current.copy(selectedCategories = values)
        }
    }

    fun setExportSelection(categories: Set<TypeTypeBackupCategory>) {
        if (_state.value.isStartingJob) return
        _state.update { it.copy(selectedCategories = categories) }
    }

    fun setDuplicatePolicy(policy: PortabilityDuplicatePolicy) =
        _state.update { it.copy(duplicatePolicy = policy) }

    fun startExport() {
        val format = _state.value.selectedFormat ?: return
        val categories = _state.value.selectedCategories
        if (_state.value.isStartingJob || categories.isEmpty()) return
        viewModelScope.launch {
            busy(true)
            portabilityRepository.startExport(format.format, categories)
                .onSuccess { watch(it.id) }
                .onFailure(::showFailure)
            busy(false)
        }
    }

    fun chooseImportFile(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                documentReader.read(uri, "portability-import")
            }.onSuccess(::uploadImport)
                .onFailure(::showFailure)
        }
    }

    fun applyImport() {
        val job = _state.value.job ?: return
        if (_state.value.isApplying || job.state != PortabilityJobState.Ready) return
        viewModelScope.launch {
            _state.update { it.clearFailure() }
            portabilityRepository.applyImport(
                jobId = job.id,
                categories = _state.value.selectedCategories,
                duplicatePolicy = _state.value.duplicatePolicy,
            ).onSuccess { updated ->
                _state.update { it.copy(job = updated) }
                watch(updated.id)
            }.onFailure(::showFailure)
            _state.update { it.copy(isApplying = false) }
        }
    }

    fun cancel() {
        val jobId = _state.value.job?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(isCancelling = true) }
            portabilityRepository.cancel(jobId)
                .onSuccess { updated -> _state.update { it.copy(job = updated) } }
                .onFailure(::showFailure)
            _state.update { it.copy(isCancelling = false) }
        }
    }

    fun downloadArtifact(destination: Uri) {
        val jobId = _state.value.job?.id ?: return
        viewModelScope.launch {
            _state.update { it.clearFailure() }
            portabilityRepository.downloadArtifact(jobId, destination.toString())
                .onSuccess { _state.update { current -> current.clearFailure() } }
                .onFailure(::showFailure)
            _state.update { it.copy(isSavingArtifact = false) }
        }
    }

    fun downloadReport(destination: Uri) {
        val jobId = _state.value.job?.id ?: return
        viewModelScope.launch {
            portabilityRepository.downloadReport(jobId, destination.toString())
                .onFailure(::showFailure)
        }
    }

    fun resetJob() {
        val jobId = _state.value.job?.id
        _state.update {
            it.clearFailure().copy(job = null, selectedCategories = emptySet())
        }
        jobId?.let { id ->
            viewModelScope.launch { portabilityRepository.delete(id) }
        }
    }

    private fun uploadImport(document: ImportDocument) {
        val format = _state.value.selectedFormat ?: return
        viewModelScope.launch {
            busy(true)
            _state.update { it.copy(selectedDocumentName = document.displayName) }
            portabilityRepository.startImport(document, format.format)
                .onSuccess { watch(it.id) }
                .onFailure(::showFailure)
            busy(false)
        }
    }

    private fun watch(jobId: String) {
        viewModelScope.launch {
            while (true) {
                portabilityRepository.job(jobId)
                    .onSuccess { updated ->
                        _state.update { it.copy(job = updated) }
                        if (updated.state == PortabilityJobState.Ready ||
                            updated.state.isTerminalState
                        ) {
                            if (updated.state == PortabilityJobState.Ready) {
                                selectPreviewCategories(updated)
                            }
                            return@launch
                        }
                    }
                    .onFailure(::showFailure)
                delay(POLL_INTERVAL_MILLIS)
            }
        }
    }

    private fun selectPreviewCategories(updated: PortabilityJob) {
        _state.update { it.copy(selectedCategories = updated.preview?.counts?.keys.orEmpty()) }
    }

    private val PortabilityJobState.isTerminalState
        get() = this == PortabilityJobState.Completed ||
            this == PortabilityJobState.Failed ||
            this == PortabilityJobState.Cancelled

    private fun busy(enabled: Boolean) = _state.update {
        if (enabled) {
            it.clearFailure().copy(isStartingJob = true)
        } else {
            it.copy(isStartingJob = false)
        }
    }

    private fun showFailure(failure: Throwable) = _state.update {
        val codedFailure = failure as? CodedFailure
        it.copy(
            isLoadingFormats = false,
            isStartingJob = false,
            isApplying = false,
            isSavingArtifact = false,
            failureMessage = failure.message ?: DEFAULT_FAILURE_MESSAGE,
            failureCode = codedFailure?.failureCode,
            failureRequestId = codedFailure?.requestId,
        )
    }

    private fun PortabilityUiState.clearFailure(): PortabilityUiState = copy(
        failureMessage = null,
        failureCode = null,
        failureRequestId = null,
    )

    private companion object {
        const val POLL_INTERVAL_MILLIS = 1_000L
        const val DEFAULT_FAILURE_MESSAGE = "Portability request failed"
    }
}
