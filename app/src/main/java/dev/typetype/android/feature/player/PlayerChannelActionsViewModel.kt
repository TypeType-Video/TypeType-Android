package dev.typetype.android.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.subscriptions.SubscriptionsRepository
import dev.typetype.android.domain.subscriptions.canonicalChannelUrl
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerChannelActionsState(
    val subscribedUrls: Set<String> = emptySet(),
    val updatingUrl: String? = null,
) {
    fun isSubscribed(channelUrl: String): Boolean =
        canonicalChannelUrl(channelUrl) in subscribedUrls

    fun isUpdating(channelUrl: String): Boolean =
        canonicalChannelUrl(channelUrl) == updatingUrl
}

@HiltViewModel
class PlayerChannelActionsViewModel @Inject constructor(
    private val subscriptionsRepository: SubscriptionsRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PlayerChannelActionsState())
    val state = mutableState.asStateFlow()

    private val mutableEvents = Channel<PlayerEvent>(Channel.BUFFERED)
    val events = mutableEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            subscriptionsRepository.observeSubscribedChannelUrls().collect { urls ->
                mutableState.update {
                    it.copy(subscribedUrls = urls.mapTo(mutableSetOf(), ::canonicalChannelUrl))
                }
            }
        }
    }

    fun toggle(stream: Stream) {
        toggle(stream.uploaderUrl, stream.uploaderName, stream.uploaderAvatarUrl)
    }

    fun toggle(channelUrl: String, name: String, avatarUrl: String) {
        val canonicalUrl = canonicalChannelUrl(channelUrl)
        if (canonicalUrl.isBlank() || mutableState.value.updatingUrl != null) return
        val subscribed = mutableState.value.isSubscribed(canonicalUrl)
        viewModelScope.launch {
            mutableState.update { it.copy(updatingUrl = canonicalUrl) }
            val result = if (subscribed) {
                subscriptionsRepository.unsubscribe(canonicalUrl)
            } else {
                subscriptionsRepository.subscribe(
                    channelUrl = canonicalUrl,
                    name = name,
                    avatarUrl = avatarUrl,
                )
            }
            mutableState.update { it.copy(updatingUrl = null) }
            result.onFailure { mutableEvents.send(PlayerEvent.ActionFailed) }
        }
    }
}
