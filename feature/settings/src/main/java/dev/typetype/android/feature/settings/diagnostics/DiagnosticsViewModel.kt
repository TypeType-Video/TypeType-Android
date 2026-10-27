package dev.typetype.android.feature.settings.diagnostics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.domain.diagnostics.DiagnosticEntry
import dev.typetype.android.domain.diagnostics.DiagnosticsRepository
import dev.typetype.android.domain.diagnostics.CrashReport
import dev.typetype.android.domain.diagnostics.CrashReportRepository
import dev.typetype.android.domain.support.SupportReportCategory
import dev.typetype.android.domain.support.SupportReportDraft
import dev.typetype.android.domain.support.SupportReportReceipt
import dev.typetype.android.domain.support.SupportRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiagnosticsState(
    val entries: List<DiagnosticEntry> = emptyList(),
    val crashReport: CrashReport? = null,
    val isLoading: Boolean = true,
    val reportAvailabilityLoaded: Boolean = false,
    val canSubmitReport: Boolean = false,
    val isReportComposerVisible: Boolean = false,
    val reportCategory: SupportReportCategory = SupportReportCategory.Functionality,
    val reportDescription: String = "",
    val isSubmitConfirmationVisible: Boolean = false,
    val isSubmittingReport: Boolean = false,
    val reportReceipt: SupportReportReceipt? = null,
    val reportErrorMessage: String? = null,
    val reportErrorRequestId: String? = null,
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val repository: DiagnosticsRepository,
    private val crashReportRepository: CrashReportRepository,
    private val supportRepository: SupportRepository,
    private val errorMapper: UserErrorMapper,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    private val _state = MutableStateFlow(DiagnosticsState())
    val state = _state.asStateFlow()

    init {
        refresh()
        loadReportAvailability()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val entries = repository.listCurrent()
            val crashReport = crashReportRepository.latestCurrent()
            _state.update {
                it.copy(entries = entries, crashReport = crashReport, isLoading = false)
            }
        }
    }

    fun clear() {
        viewModelScope.launch {
            repository.clearCurrent()
            crashReportRepository.clearCurrent()
            _state.update {
                it.copy(entries = emptyList(), crashReport = null, isLoading = false)
            }
        }
    }

    fun openReportComposer() {
        _state.update {
            it.copy(
                isReportComposerVisible = true,
                reportReceipt = null,
                reportErrorMessage = null,
                reportErrorRequestId = null,
            )
        }
    }

    fun closeReportComposer() {
        _state.update {
            it.copy(
                isReportComposerVisible = false,
                isSubmitConfirmationVisible = false,
                reportErrorMessage = null,
                reportErrorRequestId = null,
            )
        }
    }

    fun selectReportCategory(category: SupportReportCategory) {
        _state.update {
            it.copy(
                reportCategory = category,
                reportErrorMessage = null,
                reportErrorRequestId = null,
            )
        }
    }

    fun updateReportDescription(value: String) {
        if (value.length > MAX_DESCRIPTION_LENGTH) return
        _state.update {
            it.copy(
                reportDescription = value,
                reportErrorMessage = null,
                reportErrorRequestId = null,
            )
        }
    }

    fun requestReportSubmission() {
        if (_state.value.reportDescription.isBlank()) {
            _state.update {
                it.copy(
                    reportErrorMessage = context.getString(R.string.support_report_description_required),
                    reportErrorRequestId = null,
                )
            }
            return
        }
        _state.update {
            it.copy(
                isSubmitConfirmationVisible = true,
                reportErrorMessage = null,
                reportErrorRequestId = null,
            )
        }
    }

    fun dismissReportSubmission() {
        _state.update { it.copy(isSubmitConfirmationVisible = false) }
    }

    fun submitReport() {
        val snapshot = _state.value
        if (!snapshot.canSubmitReport || snapshot.isSubmittingReport) return
        val draft = SupportReportDraft(
            category = snapshot.reportCategory,
            description = snapshot.reportDescription,
            diagnostics = snapshot.entries,
        )
        _state.update {
            it.copy(
                isSubmitConfirmationVisible = false,
                isSubmittingReport = true,
                reportErrorMessage = null,
                reportErrorRequestId = null,
            )
        }
        viewModelScope.launch {
            supportRepository.submitReport(draft)
                .onSuccess { receipt ->
                    repository.clearCurrent()
                    _state.update {
                        it.copy(
                            entries = emptyList(),
                            isReportComposerVisible = false,
                            isSubmittingReport = false,
                            reportDescription = "",
                            reportReceipt = receipt,
                        )
                    }
                }
                .onFailure { failure ->
                    val details = errorMapper.details(failure, R.string.support_report_submit_failed)
                    _state.update {
                        it.copy(
                            isSubmittingReport = false,
                            reportErrorMessage = details.message,
                            reportErrorRequestId = details.requestId,
                        )
                    }
                }
        }
    }

    private fun loadReportAvailability() {
        viewModelScope.launch {
            val available = supportRepository.canSubmitReport()
            _state.update {
                it.copy(
                    reportAvailabilityLoaded = true,
                    canSubmitReport = available,
                )
            }
        }
    }

    private companion object {
        const val MAX_DESCRIPTION_LENGTH = 10_000
    }
}
