package dev.typetype.android.services

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.domain.playback.PlaybackQueueEntry
import dev.typetype.android.domain.playback.PlaybackQueueController
import dev.typetype.android.domain.playback.PlaybackQueueMutationResult
import dev.typetype.android.domain.playback.PlaybackQueueSnapshot
import dev.typetype.android.domain.playback.PlaybackQueueState
import dev.typetype.android.domain.playback.PlaybackRepeatMode
import dev.typetype.android.domain.playback.enqueueEntry
import dev.typetype.android.domain.playback.moveEntryNext
import dev.typetype.android.domain.playback.removeEntry
import dev.typetype.android.domain.playback.shuffleUpcoming
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val RETRY_DELAY_MILLIS = 15_000L

@Singleton
class PlaybackQueueCoordinator @Inject internal constructor(
    private val resolver: PlaybackQueueItemResolver,
    private val activeAccountScope: ActiveAccountScope,
    private val persistence: PlaybackQueuePersistence,
    private val autoplayController: PlaybackQueueAutoplayController,
) : Player.Listener, PlaybackQueueController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(PlaybackQueueState())
    override val state: StateFlow<PlaybackQueueState> = mutableState.asStateFlow()

    private var player: Player? = null
    private var preparationJob: Job? = null
    private var resolvedNextUrl: String? = null
    private var resolvedNextItem: MediaItem? = null
    private var resolvedNextAtMillis = 0L
    private var retryAfterMillis = 0L
    private var advanceRequestedUrl: String? = null
    private var generation = 0L
    private val ownerStore = PlaybackQueueOwnerStore(persistence)
    private val preloadTicker = PlaybackQueuePreloadTicker(
        scope = scope,
        player = { player },
        queue = { mutableState.value },
        resolvedNextUrl = { resolvedNextUrl },
        resolvedAtMillis = { resolvedNextAtMillis },
        prepareNext = ::prepareNext,
    )

    init {
        scope.launch {
            autoplayController.countdown.collect { countdown ->
                mutableState.update { it.copy(autoplayCountdown = countdown) }
            }
        }
        scope.launch {
            autoplayController.advanceRequests.collect { advanceToNext() }
        }
    }

    fun attach(player: Player) {
        if (this.player === player) return
        this.player?.removeListener(this)
        this.player = player
        player.addListener(this)
        player.applyQueueRepeatMode(mutableState.value)
        preloadTicker.start()
        autoplayController.updateFrom(player, mutableState.value)
    }

    fun detach(player: Player) {
        if (this.player !== player) return
        player.removeListener(this)
        this.player = null
        preparationJob?.cancel()
        preloadTicker.stop()
        autoplayController.updatePlaybackContext(Player.STATE_IDLE, false, null, null)
    }

    override fun start(title: String, entries: List<PlaybackQueueEntry>, shuffle: Boolean) {
        val distinct = entries.filter { it.videoUrl.isNotBlank() }.distinctBy { it.videoUrl }
        val ordered = if (shuffle) distinct.shuffled() else distinct
        if (ordered.isEmpty()) return
        invalidatePreparation()
        player?.run {
            stop()
            clearMediaItems()
        }
        mutableState.value = PlaybackQueueState(
            title = title,
            entries = ordered,
            currentIndex = 0,
        )
        player?.applyQueueRepeatMode(mutableState.value)
        autoplayController.updateFrom(player, mutableState.value)
        captureOwnerAndPersist(generation)
    }

    override fun restore(snapshot: PlaybackQueueSnapshot) {
        invalidatePreparation()
        ownerStore.restore(snapshot)
        mutableState.value = snapshot.toState()
        player?.applyQueueRepeatMode(mutableState.value)
        autoplayController.updateFrom(player, mutableState.value)
    }

    override fun clear() {
        invalidatePreparation()
        mutableState.value = PlaybackQueueState()
        player?.applyQueueRepeatMode(mutableState.value)
        player?.retainCurrentMediaItem()
        autoplayController.updateFrom(player, mutableState.value)
        clearPersistedQueue()
    }

    fun play(index: Int) {
        val current = mutableState.value
        if (index !in current.entries.indices || index == current.currentIndex) return
        invalidatePreparation()
        player?.run {
            stop()
            clearMediaItems()
        }
        mutableState.value = current.copy(
            currentIndex = index,
            isPreparingNext = false,
            failedVideoUrl = null,
        )
        autoplayController.updateFrom(player, mutableState.value)
        persistCurrentQueue()
    }

    fun retryNext() {
        retryAfterMillis = 0L
        prepareNext(forceRefresh = true)
    }

    fun advanceToNext() {
        val next = mutableState.value.next ?: return
        advanceRequestedUrl = next.videoUrl
        val prepared = resolvedNextItem?.takeIf { resolvedNextUrl == next.videoUrl }
        if (prepared != null) {
            playPreparedNext(prepared)
        } else {
            retryAfterMillis = 0L
            prepareNext(forceRefresh = true)
        }
    }

    fun playAutoplayNow() = autoplayController.playNow()

    fun cancelAutoplay() = autoplayController.cancel()

    fun toggleAutoplayPause() = autoplayController.togglePause()

    fun playNext(index: Int) = editQueue { it.moveEntryNext(index) }

    fun remove(index: Int) = editQueue { it.removeEntry(index) }

    fun shuffleUpcoming() = editQueue { it.shuffleUpcoming() }

    fun cycleRepeatMode() {
        val current = mutableState.value.takeIf(PlaybackQueueState::isActive) ?: return
        editQueue {
            current.copy(
                repeatMode = current.repeatMode.next(),
                isPreparingNext = false,
                failedVideoUrl = null,
            )
        }
    }

    fun enqueue(entry: PlaybackQueueEntry, playNext: Boolean): PlaybackQueueMutationResult {
        val current = mutableState.value
        if (current.isActive) {
            val mutation = current.enqueueEntry(entry, playNext)
            mutation.state?.let { updated -> editQueue { updated } }
            return mutation.result
        }
        val adoption = player?.adoptQueue(entry)
            ?: return PlaybackQueueMutationResult.NoActivePlayback
        val adoptedState = adoption.state ?: return adoption.result
        invalidatePreparation()
        adoption.owner?.let(ownerStore::replace)
        mutableState.value = adoptedState
        player?.applyQueueRepeatMode(adoptedState)
        autoplayController.updateFrom(player, adoptedState)
        persistCurrentQueue()
        prepareNext(forceRefresh = false)
        return adoption.result
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val videoUrl = mediaItem?.mediaId?.takeIf { it.isNotBlank() } ?: return
        val current = mutableState.value
        if (!current.isActive) return
        val index = current.entries.indexOfFirst { it.videoUrl == videoUrl }
        if (index < 0) {
            clear()
            return
        }
        mediaItem.requestMetadata.extras?.streamRequestScope()?.let { requestScope ->
            val nextOwner = AccountScope(requestScope.serverId, requestScope.accountId)
            if (ownerStore.owner != nextOwner) ownerStore.replace(nextOwner)
        }
        mutableState.value = current.copy(
            currentIndex = index,
            isPreparingNext = false,
            failedVideoUrl = null,
        )
        autoplayController.onMediaTransition()
        invalidatePreparation(advanceGeneration = false)
        persistCurrentQueue()
        prepareNext(forceRefresh = false)
        autoplayController.updateFrom(player, mutableState.value)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        val queue = mutableState.value
        autoplayController.updateFrom(player, mutableState.value, playbackState)
        if (
            playbackState == Player.STATE_ENDED && queue.repeatMode == PlaybackRepeatMode.Off &&
            queue.currentIndex == queue.entries.lastIndex
        ) {
            clearPersistedQueue(retainOwner = true)
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        autoplayController.updateFrom(player, mutableState.value)
    }

    private fun prepareNext(forceRefresh: Boolean) {
        if (player == null) return
        val queue = mutableState.value
        val next = queue.next ?: return
        val now = System.currentTimeMillis()
        if (preparationJob?.isActive == true || now < retryAfterMillis) return
        if (!forceRefresh && resolvedNextUrl == next.videoUrl && resolvedNextItem != null) return
        val requestedGeneration = generation
        val requestedIndex = queue.currentIndex
        mutableState.update { it.copy(isPreparingNext = true, failedVideoUrl = null) }
        preparationJob = scope.launch {
            resolver.resolve(next.videoUrl).fold(
                onSuccess = { item ->
                    val latest = mutableState.value
                    if (
                        generation != requestedGeneration || latest.currentIndex != requestedIndex ||
                        latest.next?.videoUrl != next.videoUrl
                    ) return@fold
                    resolvedNextUrl = next.videoUrl
                    resolvedNextItem = item
                    resolvedNextAtMillis = System.currentTimeMillis()
                    retryAfterMillis = 0L
                    mutableState.update { it.copy(isPreparingNext = false, failedVideoUrl = null) }
                    if (advanceRequestedUrl == next.videoUrl) playPreparedNext(item)
                },
                onFailure = {
                    if (generation != requestedGeneration) return@fold
                    retryAfterMillis = System.currentTimeMillis() + RETRY_DELAY_MILLIS
                    mutableState.update {
                        it.copy(isPreparingNext = false, failedVideoUrl = next.videoUrl)
                    }
                },
            )
        }
    }

    private fun playPreparedNext(item: MediaItem) {
        val target = player ?: return
        if (mutableState.value.next?.videoUrl != item.mediaId) return
        advanceRequestedUrl = null
        target.setMediaItem(item)
        target.prepare()
        target.play()
    }

    private fun editQueue(transform: (PlaybackQueueState) -> PlaybackQueueState?) {
        val updated = transform(mutableState.value) ?: return
        invalidatePreparation()
        player?.retainCurrentMediaItem()
        mutableState.value = updated
        player?.applyQueueRepeatMode(updated)
        autoplayController.updateFrom(player, mutableState.value)
        persistCurrentQueue()
        prepareNext(forceRefresh = false)
    }

    private fun invalidatePreparation(advanceGeneration: Boolean = true) {
        if (advanceGeneration) generation += 1L
        preparationJob?.cancel()
        preparationJob = null
        resolvedNextUrl = null
        resolvedNextItem = null
        resolvedNextAtMillis = 0L
        retryAfterMillis = 0L
        advanceRequestedUrl = null
    }

    private fun captureOwnerAndPersist(expectedGeneration: Long) {
        scope.launch {
            val active = activeAccountScope.observe().first() ?: return@launch
            if (generation != expectedGeneration || !mutableState.value.isActive) return@launch
            if (ownerStore.owner != active) ownerStore.replace(active)
            persistCurrentQueue()
        }
    }

    private fun persistCurrentQueue() = ownerStore.save(mutableState.value)

    private fun clearPersistedQueue(
        targetOwner: AccountScope? = ownerStore.owner,
        retainOwner: Boolean = false,
    ) = ownerStore.clear(targetOwner, retainOwner)
}
