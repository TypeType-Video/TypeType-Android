package dev.typetype.android.feature.settings.rss

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.AccountScopeProvider
import dev.typetype.android.domain.rss.RssFeed
import dev.typetype.android.domain.rss.RssFeedSecret
import dev.typetype.android.domain.rss.RssRepository
import dev.typetype.android.domain.server.RssCapability
import dev.typetype.android.domain.server.ServerRepository
import dev.typetype.android.domain.subscriptions.SubscriptionSummary
import dev.typetype.android.domain.subscriptions.SubscriptionsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RssFeedsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: RssRepository,
    private val serverRepository: ServerRepository,
    private val subscriptionsRepository: SubscriptionsRepository,
    private val accountScopeProvider: AccountScopeProvider,
    private val errorMapper: UserErrorMapper,
) : ViewModel() {
    private val mutableState = MutableStateFlow(RssFeedsState())
    val state = mutableState.asStateFlow()
    private var activeScope: AccountScope? = null

    init {
        viewModelScope.launch {
            combine(
                serverRepository.observeCurrentServer(),
                repository.observeFeeds(),
                subscriptionsRepository.observeSubscriptions(),
                accountScopeProvider.observe(),
            ) { server, feeds, subscriptions, scope ->
                val alignedScope = scope?.takeIf { it.serverId == server?.id }
                RssFeedsSnapshot(
                    server?.rss?.takeIf { alignedScope != null } ?: RssCapability(),
                    server?.supportedServices?.takeIf { alignedScope != null },
                    feeds,
                    subscriptions,
                    alignedScope,
                )
            }.collect { snapshot ->
                val scopeChanged = snapshot.scope != activeScope
                activeScope = snapshot.scope
                mutableState.update {
                    val scoped = if (scopeChanged) it.clearedForAccountChange() else it
                    scoped.copy(
                        capability = snapshot.capability,
                        availableServiceIds = snapshot.supportedServices
                            ?.filter(SUPPORTED_SERVICE_IDS::contains)
                            ?.toSet()
                            .orEmpty(),
                        feeds = if (scopeChanged) emptyList() else snapshot.feeds,
                        subscriptions = if (scopeChanged) emptyList() else snapshot.subscriptions,
                    )
                }
                if (scopeChanged && snapshot.scope != null && snapshot.capability.enabled) refresh()
            }
        }
    }

    fun onAction(action: RssFeedsAction) {
        when (action) {
            RssFeedsAction.Retry -> refresh()
            RssFeedsAction.Create -> openCreate()
            is RssFeedsAction.Edit -> openEdit(action.feedId)
            RssFeedsAction.DismissEditor -> update { copy(editor = null) }
            is RssFeedsAction.SetName -> edit { copy(name = action.name.take(100)) }
            is RssFeedsAction.SetScope -> edit { copy(scope = action.scope) }
            is RssFeedsAction.ToggleChannel -> edit {
                copy(channelUrls = channelUrls.toggle(action.channelUrl, MAX_CHANNELS))
            }
            is RssFeedsAction.ToggleService -> edit {
                copy(serviceIds = serviceIds.toggle(action.serviceId))
            }
            is RssFeedsAction.SetVideos -> edit { copy(includeVideos = action.included) }
            is RssFeedsAction.SetShorts -> edit { copy(includeShorts = action.included) }
            is RssFeedsAction.SetLive -> edit { copy(includeLive = action.included) }
            is RssFeedsAction.SetUpcoming -> edit { copy(includeUpcoming = action.included) }
            RssFeedsAction.Save -> save()
            is RssFeedsAction.SetEnabled -> mutate {
                repository.setEnabled(action.feedId, action.enabled)
            }
            is RssFeedsAction.RequestRegenerate -> update {
                copy(regeneratingFeedId = action.feedId)
            }
            RssFeedsAction.DismissRegenerate -> update { copy(regeneratingFeedId = null) }
            RssFeedsAction.ConfirmRegenerate -> regenerate()
            is RssFeedsAction.RequestDelete -> update { copy(deletingFeedId = action.feedId) }
            RssFeedsAction.DismissDelete -> update { copy(deletingFeedId = null) }
            RssFeedsAction.ConfirmDelete -> delete()
            RssFeedsAction.DismissSecret -> update { copy(secret = null) }
            RssFeedsAction.DismissFailure -> clearFailure()
        }
    }

    private fun refresh() {
        val operationScope = activeScope ?: return
        viewModelScope.launch {
            if (activeScope != operationScope) return@launch
            update { copy(isLoading = true) }
            subscriptionsRepository.refresh()
            val result = repository.refresh()
            if (activeScope != operationScope) return@launch
            result.fold(
                onSuccess = { update { copy(isLoading = false, hasLoadedFeeds = true) } },
                onFailure = { showFailure(it, isLoading = false) },
            )
        }
    }

    private fun openCreate() {
        if (!mutableState.value.canCreate) return
        val services = mutableState.value.availableServiceIds
        update {
            copy(
                editor = RssFeedEditorState(
                    name = context.getString(R.string.rss_default_name),
                    serviceIds = services,
                ),
            )
        }
    }

    private fun openEdit(feedId: String) {
        val feed = mutableState.value.feeds.firstOrNull { it.id == feedId } ?: return
        update { copy(editor = feed.toEditorState()) }
    }

    private fun save() {
        val editor = mutableState.value.editor ?: return
        val validation = editor.validationError()
        if (validation != null) {
            update { copy(errorMessage = context.getString(validation), errorRequestId = null) }
            return
        }
        mutate(
            onSuccess = { secret ->
                update {
                    copy(
                        editor = null,
                        secret = secret?.let { RssFeedSecretState(it.feed.name, it.url) },
                    )
                }
            },
        ) {
            if (editor.feedId == null) {
                repository.create(editor.toDraft())
            } else {
                repository.update(editor.feedId, editor.toDraft()).map { null }
            }
        }
    }

    private fun regenerate() {
        val feedId = mutableState.value.regeneratingFeedId ?: return
        mutate(onSuccess = { secret: RssFeedSecret ->
            update {
                copy(
                    regeneratingFeedId = null,
                    secret = RssFeedSecretState(secret.feed.name, secret.url),
                )
            }
        }) {
            repository.regenerate(feedId)
        }
    }

    private fun delete() {
        val feedId = mutableState.value.deletingFeedId ?: return
        mutate(onSuccess = { update { copy(deletingFeedId = null) } }) {
            repository.delete(feedId).map { null }
        }
    }

    private fun <T> mutate(
        onSuccess: (T) -> Unit = {},
        block: suspend () -> Result<T>,
    ) {
        if (mutableState.value.isMutating) return
        val operationScope = activeScope ?: return
        viewModelScope.launch {
            if (activeScope != operationScope) return@launch
            update { copy(isMutating = true, errorMessage = null, errorRequestId = null) }
            val result = block()
            if (activeScope != operationScope) return@launch
            result.fold(
                onSuccess = {
                    update { copy(isMutating = false) }
                    onSuccess(it)
                },
                onFailure = { showFailure(it, isMutating = false) },
            )
        }
    }

    private fun edit(transform: RssFeedEditorState.() -> RssFeedEditorState) {
        update { copy(editor = editor?.transform()) }
    }

    private fun showFailure(
        failure: Throwable,
        isLoading: Boolean = mutableState.value.isLoading,
        isMutating: Boolean = mutableState.value.isMutating,
    ) {
        val details = errorMapper.details(failure, R.string.rss_error_generic)
        update {
            copy(
                isLoading = isLoading,
                isMutating = isMutating,
                errorMessage = details.message,
                errorRequestId = details.requestId,
            )
        }
    }

    private fun clearFailure() = update { copy(errorMessage = null, errorRequestId = null) }
    private fun update(transform: RssFeedsState.() -> RssFeedsState) = mutableState.update(transform)

    private fun <T> Set<T>.toggle(value: T, limit: Int = Int.MAX_VALUE): Set<T> = when {
        value in this -> this - value
        size < limit -> this + value
        else -> this
    }

    private companion object {
        val SUPPORTED_SERVICE_IDS = setOf(0, 5, 6)
        const val MAX_CHANNELS = 100
    }
}

private data class RssFeedsSnapshot(
    val capability: RssCapability,
    val supportedServices: List<Int>?,
    val feeds: List<RssFeed>,
    val subscriptions: List<SubscriptionSummary>,
    val scope: AccountScope?,
)
