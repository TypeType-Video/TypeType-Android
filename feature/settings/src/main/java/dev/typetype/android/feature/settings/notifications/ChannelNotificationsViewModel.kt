package dev.typetype.android.feature.settings.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.core.error.suspendRunCatching
import dev.typetype.android.domain.push.PushRepository
import dev.typetype.android.domain.push.matchesChannel
import dev.typetype.android.domain.subscriptions.SubscriptionsRepository
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class ChannelBell(
    val channelUrl: String,
    val name: String,
    val avatarUrl: String,
    val enabled: Boolean,
    val pending: Boolean = false,
)

data class ChannelNotificationsState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val channels: List<ChannelBell> = emptyList(),
    val bulkInFlight: Boolean = false,
    val bulkDone: Int = 0,
    val bulkTotal: Int = 0,
    val showError: Boolean = false,
) {
    val enabledCount: Int get() = channels.count { it.enabled }
}

sealed interface ChannelNotificationsAction {
    data object Retry : ChannelNotificationsAction

    data class Toggle(val channelUrl: String) : ChannelNotificationsAction

    data class SetAll(val enabled: Boolean) : ChannelNotificationsAction
}

@HiltViewModel
class ChannelNotificationsViewModel @Inject constructor(
    private val subscriptions: SubscriptionsRepository,
    private val pushRepository: PushRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ChannelNotificationsState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun onAction(action: ChannelNotificationsAction) {
        when (action) {
            ChannelNotificationsAction.Retry -> load()
            is ChannelNotificationsAction.Toggle -> toggle(action.channelUrl)
            is ChannelNotificationsAction.SetAll -> setAll(action.enabled)
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadFailed = false, showError = false) }
            val channels = suspendRunCatching {
                val subscribed = subscriptions.listSubscriptions().getOrThrow()
                val bells = pushRepository.channelPreferences().getOrThrow().filter { it.enabled }
                subscribed
                    .sortedBy { it.name.lowercase() }
                    .map { channel ->
                        ChannelBell(
                            channelUrl = channel.channelUrl,
                            name = channel.name,
                            avatarUrl = channel.avatarUrl,
                            enabled = bells.any { it.matchesChannel(channel.channelUrl) },
                        )
                    }
            }
            _state.update {
                it.copy(
                    isLoading = false,
                    loadFailed = channels.isFailure,
                    channels = channels.getOrDefault(it.channels),
                )
            }
        }
    }

    private fun toggle(channelUrl: String) {
        val current = _state.value
        val channel = current.channels.firstOrNull { it.channelUrl == channelUrl } ?: return
        if (channel.pending || current.bulkInFlight) return
        viewModelScope.launch {
            val saved = save(channel, !channel.enabled)
            if (!saved) _state.update { it.copy(showError = true) }
        }
    }

    private fun setAll(enabled: Boolean) {
        val current = _state.value
        if (current.bulkInFlight) return
        val targets = current.channels.filter { it.enabled != enabled && !it.pending }
        if (targets.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(bulkInFlight = true, bulkDone = 0, bulkTotal = targets.size, showError = false) }
            val saved = when (saveAllAtOnce(targets, enabled)) {
                BulkSave.Saved -> true
                BulkSave.Failed -> false
                BulkSave.Unsupported -> saveOneByOne(targets, enabled)
            }
            _state.update { it.copy(bulkInFlight = false, showError = !saved) }
        }
    }

    private suspend fun saveAllAtOnce(targets: List<ChannelBell>, enabled: Boolean): BulkSave {
        val urls = targets.mapTo(hashSetOf(), ChannelBell::channelUrl)
        _state.update { state ->
            state.copy(channels = state.channels.map { if (it.channelUrl in urls) it.copy(pending = true) else it })
        }
        val failure = suspendRunCatching {
            pushRepository.setChannelPreferences(urls.toList(), enabled).getOrThrow()
        }.exceptionOrNull()
        val result = when {
            failure == null -> BulkSave.Saved
            (failure as? CodedFailure)?.statusCode in BULK_UNSUPPORTED_STATUSES -> BulkSave.Unsupported
            else -> BulkSave.Failed
        }
        _state.update { state ->
            state.copy(
                bulkDone = if (result == BulkSave.Saved) state.bulkTotal else state.bulkDone,
                channels = state.channels.map { channel ->
                    if (channel.channelUrl !in urls) return@map channel
                    channel.copy(enabled = if (result == BulkSave.Saved) enabled else channel.enabled, pending = false)
                },
            )
        }
        return result
    }

    private suspend fun saveOneByOne(targets: List<ChannelBell>, enabled: Boolean): Boolean {
        val gate = Semaphore(BULK_PARALLELISM)
        val results = coroutineScope {
            targets.map { channel ->
                async {
                    gate.withPermit { save(channel, enabled) }
                        .also { _state.update { it.copy(bulkDone = it.bulkDone + 1) } }
                }
            }.awaitAll()
        }
        return results.all { it }
    }

    private suspend fun save(channel: ChannelBell, enabled: Boolean): Boolean {
        updateChannel(channel.channelUrl) { it.copy(enabled = enabled, pending = true) }
        val saved = sendWithRetry(channel.channelUrl, enabled)
        updateChannel(channel.channelUrl) {
            it.copy(enabled = if (saved) enabled else !enabled, pending = false)
        }
        return saved
    }

    private suspend fun sendWithRetry(channelUrl: String, enabled: Boolean): Boolean {
        repeat(MAX_ATTEMPTS) {
            val result = suspendRunCatching {
                pushRepository.setChannelPreference(channelUrl, enabled).getOrThrow()
            }
            val failure = result.exceptionOrNull() ?: return true
            if ((failure as? CodedFailure)?.statusCode != HTTP_TOO_MANY_REQUESTS) return false
            delay(RATE_LIMIT_PAUSE_MS)
        }
        return false
    }

    private fun updateChannel(channelUrl: String, transform: (ChannelBell) -> ChannelBell) {
        _state.update { state ->
            state.copy(
                channels = state.channels.map { if (it.channelUrl == channelUrl) transform(it) else it },
            )
        }
    }

    private enum class BulkSave { Saved, Failed, Unsupported }

    private companion object {
        val BULK_UNSUPPORTED_STATUSES = setOf(404, 405)
        const val BULK_PARALLELISM = 4
        const val MAX_ATTEMPTS = 5
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val RATE_LIMIT_PAUSE_MS = 20_000L
    }
}
