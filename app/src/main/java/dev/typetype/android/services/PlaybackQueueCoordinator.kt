package dev.typetype.android.services

import androidx.media3.common.C
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TICK_INTERVAL_MILLIS = 1_000L
private const val RESOLUTION_MAX_AGE_MILLIS = 120_000L
private const val REFRESH_WINDOW_MILLIS = 60_000L
private const val RETRY_DELAY_MILLIS = 15_000L

@Singleton
class PlaybackQueueCoordinator @Inject constructor(
    private val resolver: PlaybackQueueItemResolver,
    private val activeAccountScope: ActiveAccountScope,
    private val persistence: PlaybackQueuePersistence,
) : Player.Listener, PlaybackQueueController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(PlaybackQueueState())
    override val state: StateFlow<PlaybackQueueState> = mutableState.asStateFlow()

    private var player: Player? = null
    private var preparationJob: Job? = null
    private var tickerJob: Job? = null
    private var resolvedNextUrl: String? = null
    private var resolvedNextAtMillis = 0L
    private var retryAfterMillis = 0L
    private var generation = 0L
    private var owner: AccountScope? = null

    fun attach(player: Player) {
        if (this.player === player) return
        this.player?.removeListener(this)
        this.player = player
        player.addListener(this)
        player.applyQueueRepeatMode(mutableState.value)
        startTicker()
    }

    fun detach(player: Player) {
        if (this.player !== player) return
        player.removeListener(this)
        this.player = null
        preparationJob?.cancel()
        tickerJob?.cancel()
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
        captureOwnerAndPersist(generation)
    }

    override fun restore(snapshot: PlaybackQueueSnapshot) {
        invalidatePreparation()
        owner = AccountScope(snapshot.serverId, snapshot.accountId)
        mutableState.value = snapshot.toState()
        player?.applyQueueRepeatMode(mutableState.value)
        persistence.save(snapshot)
    }

    override fun clear() {
        invalidatePreparation()
        mutableState.value = PlaybackQueueState()
        player?.applyQueueRepeatMode(mutableState.value)
        player?.retainCurrentMediaItem()
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
        persistCurrentQueue()
    }

    fun retryNext() {
        retryAfterMillis = 0L
        prepareNext(forceRefresh = true)
    }

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
        adoption.owner?.let(::replaceOwner)
        mutableState.value = adoptedState
        player?.applyQueueRepeatMode(adoptedState)
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
            if (owner != nextOwner) replaceOwner(nextOwner)
        }
        mutableState.value = current.copy(
            currentIndex = index,
            isPreparingNext = false,
            failedVideoUrl = null,
        )
        invalidatePreparation(advanceGeneration = false)
        removeFinishedItems()
        persistCurrentQueue()
        prepareNext(forceRefresh = false)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        val queue = mutableState.value
        if (
            playbackState == Player.STATE_ENDED && queue.repeatMode == PlaybackRepeatMode.Off &&
            queue.currentIndex == queue.entries.lastIndex
        ) {
            clearPersistedQueue(retainOwner = true)
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(TICK_INTERVAL_MILLIS)
                val currentPlayer = player ?: continue
                val queue = mutableState.value
                if (!queue.isActive || queue.next == null) continue
                val duration = currentPlayer.duration
                val remaining = if (duration == C.TIME_UNSET || duration <= 0L) {
                    Long.MAX_VALUE
                } else {
                    duration - currentPlayer.currentPosition
                }
                val stale = resolvedNextAtMillis > 0L &&
                    System.currentTimeMillis() - resolvedNextAtMillis >= RESOLUTION_MAX_AGE_MILLIS
                when {
                    resolvedNextUrl == null -> prepareNext(forceRefresh = false)
                    stale && remaining <= REFRESH_WINDOW_MILLIS -> prepareNext(forceRefresh = true)
                }
            }
        }
    }

    private fun prepareNext(forceRefresh: Boolean) {
        val currentPlayer = player ?: return
        val queue = mutableState.value
        val next = queue.next ?: return
        val now = System.currentTimeMillis()
        if (preparationJob?.isActive == true || now < retryAfterMillis) return
        val existingIndex = currentPlayer.indexOfMediaId(next.videoUrl)
        if (!forceRefresh && existingIndex >= 0) {
            resolvedNextUrl = next.videoUrl
            return
        }
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
                    val target = player ?: return@fold
                    val replacementIndex = target.indexOfMediaId(next.videoUrl)
                    if (replacementIndex >= 0) target.replaceMediaItem(replacementIndex, item)
                    else target.addMediaItem(item)
                    resolvedNextUrl = next.videoUrl
                    resolvedNextAtMillis = System.currentTimeMillis()
                    retryAfterMillis = 0L
                    mutableState.update { it.copy(isPreparingNext = false, failedVideoUrl = null) }
                    target.resumeQueueCycleIfNeeded(latest)
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

    private fun removeFinishedItems() {
        val currentPlayer = player ?: return
        val currentIndex = currentPlayer.currentMediaItemIndex
        if (currentIndex > 0) currentPlayer.removeMediaItems(0, currentIndex)
    }

    private fun editQueue(transform: (PlaybackQueueState) -> PlaybackQueueState?) {
        val updated = transform(mutableState.value) ?: return
        invalidatePreparation()
        player?.retainCurrentMediaItem()
        mutableState.value = updated
        player?.applyQueueRepeatMode(updated)
        persistCurrentQueue()
        prepareNext(forceRefresh = false)
    }

    private fun invalidatePreparation(advanceGeneration: Boolean = true) {
        if (advanceGeneration) generation += 1L
        preparationJob?.cancel()
        resolvedNextUrl = null
        resolvedNextAtMillis = 0L
        retryAfterMillis = 0L
    }

    private fun captureOwnerAndPersist(expectedGeneration: Long) {
        scope.launch {
            val active = activeAccountScope.observe().first() ?: return@launch
            if (generation != expectedGeneration || !mutableState.value.isActive) return@launch
            if (owner != active) replaceOwner(active)
            persistCurrentQueue()
        }
    }

    private fun replaceOwner(nextOwner: AccountScope) {
        val previousOwner = owner
        owner = nextOwner
        if (previousOwner != null && previousOwner != nextOwner) {
            clearPersistedQueue(previousOwner, retainOwner = true)
        }
    }

    private fun persistCurrentQueue() {
        val currentOwner = owner ?: return
        val current = mutableState.value.takeIf(PlaybackQueueState::isActive) ?: return
        val snapshot = PlaybackQueueSnapshot(
            serverId = currentOwner.serverId,
            accountId = currentOwner.accountId,
            title = current.title,
            entries = current.entries,
            currentIndex = current.currentIndex,
            repeatMode = current.repeatMode,
            updatedAtMillis = System.currentTimeMillis(),
        )
        persistence.save(snapshot)
    }

    private fun clearPersistedQueue(
        targetOwner: AccountScope? = owner,
        retainOwner: Boolean = false,
    ) {
        val currentOwner = targetOwner ?: return
        if (!retainOwner && owner == currentOwner) owner = null
        persistence.clear(currentOwner)
    }
}
