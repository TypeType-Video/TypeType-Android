package dev.typetype.android.feature.player

import dev.typetype.android.data.network.PlaybackNetworkState
import dev.typetype.android.feature.player.error.StreamErrorKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal fun observePlayerStreamNetworkRecovery(
    scope: CoroutineScope,
    playerStates: StateFlow<PlayerState>,
    networkStates: StateFlow<PlaybackNetworkState>,
    retryStream: (String) -> Unit,
) {
    val gate = PlayerStreamNetworkRetryGate()
    scope.launch {
        combine(playerStates, networkStates, gate::retryUrl)
            .collect { url -> url?.let(retryStream) }
    }
}

internal class PlayerStreamNetworkRetryGate {
    private var lastAttempt: Attempt? = null

    fun retryUrl(player: PlayerState, network: PlaybackNetworkState): String? {
        if (!network.isAvailable || player.isLoading || player.stream != null ||
            player.error?.kind != StreamErrorKind.NetworkUnavailable
        ) return null
        val url = player.videoUrl.takeIf(String::isNotBlank) ?: return null
        val attempt = Attempt(network.generation, url)
        if (attempt == lastAttempt) return null
        lastAttempt = attempt
        return url
    }

    private data class Attempt(val networkGeneration: Long, val videoUrl: String)
}
