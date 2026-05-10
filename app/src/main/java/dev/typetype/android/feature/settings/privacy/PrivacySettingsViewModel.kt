package dev.typetype.android.feature.settings.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.data.library.LibraryNetworkSource
import dev.typetype.android.domain.library.LibraryRepository
import dev.typetype.android.domain.searchhistory.SearchHistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PrivacyState(
    val historyCount: Int = 0,
    val searchHistoryCount: Int = 0,
    val subscriptionsCount: Int = 0,
    val errorMessage: String? = null,
)

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val networkSource: LibraryNetworkSource,
) : ViewModel() {

    private val subscriptionsCount = MutableStateFlow(0)

    val state = combine(
        libraryRepository.observeHistory().map { it.size },
        subscriptionsCount,
    ) { historyCount, subsCount ->
        PrivacyState(
            historyCount = historyCount,
            subscriptionsCount = subsCount,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PrivacyState())

    private val _localState = MutableStateFlow(PrivacyState())
    val combinedState = combine(state, _localState) { server, local ->
        server.copy(
            searchHistoryCount = local.searchHistoryCount,
            errorMessage = local.errorMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PrivacyState())

    init {
        refreshSearchHistory()
        refreshSubscriptions()
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            libraryRepository.clearHistory()
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            runCatching { networkSource.deleteAllSearchHistory() }
                .onSuccess { _localState.update { it.copy(searchHistoryCount = 0) } }
                .onFailure { e -> _localState.update { it.copy(errorMessage = e.message) } }
        }
    }

    fun unsubscribeAll() {
        viewModelScope.launch {
            val subs = runCatching { networkSource.fetchSubscriptions() }.getOrDefault(emptyList())
            for (sub in subs) {
                runCatching { networkSource.deleteSubscription(sub.channelUrl) }
            }
            subscriptionsCount.value = 0
        }
    }

    private fun refreshSearchHistory() {
        viewModelScope.launch {
            searchHistoryRepository.loadHistory().onSuccess { history ->
                _localState.update { it.copy(searchHistoryCount = history.size) }
            }
        }
    }

    private fun refreshSubscriptions() {
        viewModelScope.launch {
            runCatching { networkSource.fetchSubscriptions() }
                .onSuccess { subscriptionsCount.value = it.size }
        }
    }
}
