package dev.typetype.android.services

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import dev.typetype.android.domain.stream.AudioOnlyStreamRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class ProviderAudioOnlyServiceBridge(
    private val player: Player,
    private val repository: AudioOnlyStreamRepository,
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
) : Player.Listener, PlaybackAudioOnlyController, AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val renewalGate = ProviderAudioOnlyRenewalGate()
    private var originalItem: MediaItem? = null
    private var activeMediaId: String? = null
    private var pendingIdentity: ProviderAudioOnlyIdentity? = null
    private var pendingJob: Job? = null
    private var pendingCompletion: ((Result<Unit>) -> Unit)? = null
    private var operationGeneration = 0L

    init {
        player.addListener(this)
    }

    override fun setAudioOnly(enabled: Boolean, complete: (Result<Unit>) -> Unit) {
        val current = player.currentMediaItem
        if (current == null) {
            complete(Result.failure(AudioOnlyInactivePlaybackFailure()))
            return
        }
        if (enabled == current.isProviderAudioOnly()) {
            complete(Result.success(Unit))
            return
        }
        if (enabled) enable(current, complete) else disable(current, complete)
    }

    private fun enable(current: MediaItem, complete: (Result<Unit>) -> Unit) {
        val request = current.providerAudioOnlyRequest()
        if (request == null) {
            complete(Result.failure(AudioOnlyUnavailableFailure()))
            return
        }
        resolve(
            expected = current.providerAudioOnlyIdentity(),
            request = request,
            rememberOriginal = true,
            complete = complete,
        )
    }

    private fun disable(current: MediaItem, complete: (Result<Unit>) -> Unit) {
        val original = originalItem?.takeIf { it.mediaId == current.mediaId }
        if (!current.isProviderAudioOnly() || original == null) {
            complete(Result.failure(AudioOnlyInactivePlaybackFailure()))
            return
        }
        cancelPending()
        applyMediaItem(original)
        clearActivePlayback()
        complete(Result.success(Unit))
    }

    private fun resolve(
        expected: ProviderAudioOnlyIdentity,
        request: ProviderAudioOnlyRequest,
        rememberOriginal: Boolean,
        complete: ((Result<Unit>) -> Unit)?,
    ) {
        cancelPending()
        val operation = ++operationGeneration
        pendingIdentity = expected
        pendingCompletion = complete
        val job = scope.launch(start = CoroutineStart.LAZY) {
            val result = try {
                repository.resolve(
                    requestScope = request.requestScope,
                    videoUrl = request.videoUrl,
                    preferOriginal = request.preferOriginal,
                    preferredLocale = request.preferredLocale,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(error)
            }
            if (operation != operationGeneration) return@launch
            pendingJob = null
            pendingIdentity = null
            pendingCompletion = null
            val current = player.currentMediaItem
            val outcome = result.mapCatching { stream ->
                if (current?.providerAudioOnlyIdentity() != expected) {
                    throw AudioOnlyInactivePlaybackFailure()
                }
                if (rememberOriginal) originalItem = current
                activeMediaId = current.mediaId
                applyMediaItem(current.withProviderAudioOnly(stream))
            }
            renewalGate.finish()
            complete?.invoke(outcome)
        }
        pendingJob = job
        job.start()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val identity = mediaItem?.providerAudioOnlyIdentity()
        if (pendingIdentity != null && pendingIdentity != identity) cancelPending()
        val providerMediaId = mediaItem?.takeIf { it.isProviderAudioOnly() }?.mediaId
        renewalGate.transition(providerMediaId)
        if (providerMediaId == null) {
            clearActivePlayback()
        } else if (activeMediaId != providerMediaId) {
            originalItem = null
            activeMediaId = providerMediaId
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        val current = player.currentMediaItem ?: return
        if (!current.isProviderAudioOnly() || !error.hasHttpResponseCode(401)) return
        if (!renewalGate.begin(current.mediaId, elapsedRealtime())) return
        val request = current.providerAudioOnlyRequest()
        if (request == null) {
            renewalGate.finish()
            return
        }
        resolve(
            expected = current.providerAudioOnlyIdentity(),
            request = request,
            rememberOriginal = false,
            complete = null,
        )
    }

    private fun applyMediaItem(item: MediaItem) {
        val positionMs = player.currentPosition.coerceAtLeast(0L)
        val playWhenReady = player.playWhenReady
        player.setMediaItem(item, positionMs)
        player.prepare()
        player.playWhenReady = playWhenReady
    }

    private fun cancelPending() {
        val completion = pendingCompletion
        pendingCompletion = null
        pendingIdentity = null
        operationGeneration++
        pendingJob?.cancel(CancellationException("Playback source changed"))
        pendingJob = null
        renewalGate.finish()
        completion?.invoke(Result.failure(AudioOnlyInactivePlaybackFailure()))
    }

    private fun clearActivePlayback() {
        originalItem = null
        activeMediaId = null
    }

    override fun close() {
        player.removeListener(this)
        cancelPending()
        clearActivePlayback()
        scope.cancel()
    }
}

private data class ProviderAudioOnlyIdentity(
    val mediaId: String,
    val uri: String?,
    val sourceKey: String?,
    val audioOnly: Boolean,
)

private fun MediaItem.providerAudioOnlyIdentity() = ProviderAudioOnlyIdentity(
    mediaId = mediaId,
    uri = localConfiguration?.uri?.toString(),
    sourceKey = requestMetadata.extras?.getString(MergedStreamMediaKeys.EXTRA_SOURCE_KEY),
    audioOnly = isProviderAudioOnly(),
)

@OptIn(UnstableApi::class)
private fun Throwable.hasHttpResponseCode(expectedCode: Int): Boolean {
    var current: Throwable? = this
    repeat(8) {
        val response = current as? HttpDataSource.InvalidResponseCodeException
        if (response?.responseCode == expectedCode) return true
        current = current?.cause?.takeUnless { it === current }
    }
    return false
}
