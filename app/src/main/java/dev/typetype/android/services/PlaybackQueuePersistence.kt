package dev.typetype.android.services

import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.domain.playback.PlaybackQueueRepository
import dev.typetype.android.domain.playback.PlaybackQueueSnapshot
import dev.typetype.android.domain.usersettings.UserSettings
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class PlaybackQueuePersistence @Inject constructor(
    private val repository: PlaybackQueueRepository,
    userSettingsRepository: UserSettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()
    private var revision = 0L
    private var enabled = false
    private var current: PlaybackQueueSnapshot? = null

    init {
        scope.launch {
            userSettingsRepository.current().onSuccess(::updatePolicy)
            userSettingsRepository.observe().collect(::updatePolicy)
        }
    }

    fun save(snapshot: PlaybackQueueSnapshot) {
        current = snapshot
        if (!enabled) return
        val requestedRevision = ++revision
        scope.launch {
            mutex.withLock {
                if (requestedRevision != revision || current != snapshot || !enabled) return@withLock
                repository.save(snapshot)
            }
        }
    }

    fun clear(owner: AccountScope, forget: Boolean = true) {
        if (forget && current?.belongsTo(owner) == true) current = null
        revision += 1L
        scope.launch {
            mutex.withLock { repository.clear(owner.serverId, owner.accountId) }
        }
    }

    private fun updatePolicy(settings: UserSettings) {
        val nextEnabled = !settings.disableWatchHistory
        if (enabled == nextEnabled) return
        enabled = nextEnabled
        val snapshot = current ?: return
        if (enabled) save(snapshot) else clear(snapshot.owner(), forget = false)
    }

    private fun PlaybackQueueSnapshot.owner() = AccountScope(serverId, accountId)

    private fun PlaybackQueueSnapshot.belongsTo(owner: AccountScope): Boolean =
        serverId == owner.serverId && accountId == owner.accountId
}
