package dev.typetype.android.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.domain.notifications.NotificationBadge
import dev.typetype.android.domain.notifications.NotificationsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltViewModel
class NotificationsBadgeViewModel @Inject constructor(
    private val repository: NotificationsRepository,
    activeAccountScope: ActiveAccountScope,
) : ViewModel() {
    private val refreshMutex = Mutex()
    private var currentScope: AccountScope? = null
    private var lastRefreshScope: AccountScope? = null
    private var lastRefreshNanos = 0L

    val state = repository.observeBadge().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = NotificationBadge(),
    )

    init {
        viewModelScope.launch {
            activeAccountScope.observe().collectLatest { scope ->
                currentScope = scope
                if (scope == null) return@collectLatest
                refreshBadge(scope)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { currentScope?.let { refreshBadge(it) } }
    }

    private suspend fun refreshBadge(scope: AccountScope) = refreshMutex.withLock {
        val now = System.nanoTime()
        if (
            scope == lastRefreshScope &&
            lastRefreshNanos != 0L &&
            now - lastRefreshNanos < MIN_REFRESH_INTERVAL_NANOS
        ) {
            return@withLock
        }
        lastRefreshScope = scope
        lastRefreshNanos = now
        repository.refreshUnreadCount()
    }

    private companion object {
        const val MIN_REFRESH_INTERVAL_NANOS = 1_000_000_000L
    }
}
