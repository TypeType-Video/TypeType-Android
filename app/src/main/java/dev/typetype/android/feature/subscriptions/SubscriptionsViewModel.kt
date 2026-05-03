package dev.typetype.android.feature.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.feed.HomeFeedRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val feedRepository: HomeFeedRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SubscriptionsState(isLoading = true))
    val state = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun onAction(action: SubscriptionsAction) {
        when (action) {
            SubscriptionsAction.OnRefresh -> load()
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            feedRepository.loadSubscriptionsFeed().fold(
                onSuccess = { videos ->
                    _state.update { it.copy(isLoading = false, videos = videos) }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.message) }
                },
            )
        }
    }
}
