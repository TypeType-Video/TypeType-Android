package dev.typetype.android.services

import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.data.network.PlaybackNetworkState

internal sealed interface PlaybackNetworkRecoveryAction {
    data object Wait : PlaybackNetworkRecoveryAction

    data class RetryAfter(val delayMs: Long) : PlaybackNetworkRecoveryAction
}

internal class PlaybackNetworkRecoveryGate {
    private var mediaId: String? = null
    private var pending = false
    private var networkGeneration = Long.MIN_VALUE
    private var attempt = 0

    fun transition(nextMediaId: String?) {
        if (nextMediaId == mediaId) return
        mediaId = nextMediaId
        pending = false
        networkGeneration = Long.MIN_VALUE
        attempt = 0
    }

    fun failed(
        failedMediaId: String,
        network: PlaybackNetworkState,
    ): PlaybackNetworkRecoveryAction {
        transition(failedMediaId)
        pending = true
        if (networkGeneration != network.generation) {
            networkGeneration = network.generation
            attempt = 0
        }
        if (!network.isAvailable) return PlaybackNetworkRecoveryAction.Wait
        val delayMs = RETRY_DELAYS_MS.getOrNull(attempt)
            ?: return PlaybackNetworkRecoveryAction.Wait
        attempt++
        return PlaybackNetworkRecoveryAction.RetryAfter(delayMs)
    }

    fun networkChanged(network: PlaybackNetworkState): PlaybackNetworkRecoveryAction {
        if (!pending || !network.isAvailable || network.generation == networkGeneration) {
            return PlaybackNetworkRecoveryAction.Wait
        }
        networkGeneration = network.generation
        attempt = 1
        return PlaybackNetworkRecoveryAction.RetryAfter(0L)
    }

    fun recovered() {
        pending = false
        attempt = 0
    }

    fun isPending(expectedMediaId: String): Boolean =
        pending && mediaId == expectedMediaId
}

internal fun PlaybackException.isNetworkPlaybackFailure(): Boolean =
    isNetworkPlaybackFailure(errorCode, this)

internal fun isNetworkPlaybackFailure(errorCode: Int, failure: Throwable): Boolean {
    if (failure.hasCodedFailure()) return false
    return when (errorCode) {
        PlaybackException.ERROR_CODE_TIMEOUT,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        -> true
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> failure.hasHttpTransportFailure()
        else -> false
    }
}

private fun Throwable.hasCodedFailure(): Boolean {
    var current: Throwable? = this
    repeat(MAX_CAUSE_DEPTH) {
        if (current is CodedFailure) return true
        current = current?.cause?.takeUnless { it === current }
    }
    return false
}

@OptIn(UnstableApi::class)
private fun Throwable.hasHttpTransportFailure(): Boolean {
    var current: Throwable? = this
    repeat(MAX_CAUSE_DEPTH) {
        if (current is HttpDataSource.InvalidResponseCodeException) return false
        if (
            current is HttpDataSource.HttpDataSourceException &&
            current !is HttpDataSource.InvalidContentTypeException &&
            current !is HttpDataSource.CleartextNotPermittedException
        ) {
            return true
        }
        current = current?.cause?.takeUnless { it === current }
    }
    return false
}

private val RETRY_DELAYS_MS = longArrayOf(
    1_000L,
    3_000L,
    10_000L,
    30_000L,
    60_000L,
    60_000L,
    60_000L,
    60_000L,
)
private const val MAX_CAUSE_DEPTH = 8
