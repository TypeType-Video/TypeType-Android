package dev.typetype.android.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.data.diagnostics.LocalDiagnosticsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class PlaybackNetworkMonitor @Inject constructor(
    @ApplicationContext context: Context,
    private val diagnosticsRepository: LocalDiagnosticsRepository,
) : PlaybackNetworkObserver {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val handler = Handler(Looper.getMainLooper())
    private val lock = Any()
    private var activeNetwork: Network? = connectivityManager.activeNetwork
    private var activeCapabilities = activeNetwork?.let(connectivityManager::getNetworkCapabilities)
    private var blocked = false
    private val stateTracker = PlaybackNetworkStateTracker(currentRoute())
    private val mutableStates = MutableStateFlow(stateTracker.state)

    internal val states: StateFlow<PlaybackNetworkState> = mutableStates

    override fun snapshot(): PlaybackNetworkState = mutableStates.value

    override suspend fun awaitAvailableAfter(
        generation: Long,
        timeoutMs: Long,
    ): Boolean = withTimeoutOrNull(timeoutMs) {
        states.filter { it.isAvailable && it.generation != generation }.first()
        true
    } ?: false

    private val defaultNetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateNetwork(network, capabilities = null, isBlocked = false)
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            updateActiveNetworkDetails(network, networkCapabilities, blocked)
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            signalRouteChange(network)
        }

        override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
            updateActiveNetworkDetails(network, activeCapabilities, blocked)
        }

        override fun onLost(network: Network) {
            clearNetwork(network)
        }
    }

    private val legacyNetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            refreshLegacyNetwork()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            refreshLegacyNetwork()
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            refreshLegacyNetwork()
        }

        override fun onLost(network: Network) {
            refreshLegacyNetwork()
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(defaultNetworkCallback)
        } else {
            connectivityManager.registerNetworkCallback(
                NetworkRequest.Builder()
                    .build(),
                legacyNetworkCallback,
            )
        }
    }

    private fun refreshLegacyNetwork() {
        handler.post {
            val network = connectivityManager.activeNetwork
            updateNetwork(
                network = network,
                capabilities = network?.let(connectivityManager::getNetworkCapabilities),
                isBlocked = false,
            )
        }
    }

    private fun updateNetwork(
        network: Network?,
        capabilities: NetworkCapabilities?,
        isBlocked: Boolean,
    ) {
        synchronized(lock) {
            activeNetwork = network
            activeCapabilities = capabilities
            blocked = isBlocked
            publish(stateTracker.update(currentRoute()))
        }
    }

    private fun signalRouteChange(network: Network) {
        synchronized(lock) {
            if (network != activeNetwork) return
            publish(stateTracker.update(currentRoute(), routeSignaled = true))
        }
    }

    private fun updateActiveNetworkDetails(
        network: Network,
        capabilities: NetworkCapabilities?,
        isBlocked: Boolean,
    ) {
        synchronized(lock) {
            if (network != activeNetwork) return
            activeCapabilities = capabilities
            blocked = isBlocked
            publish(stateTracker.update(currentRoute()))
        }
    }

    private fun clearNetwork(network: Network) {
        synchronized(lock) {
            if (network != activeNetwork) return
            activeNetwork = null
            activeCapabilities = null
            blocked = false
            publish(stateTracker.update(currentRoute()))
        }
    }

    private fun publish(next: PlaybackNetworkState?) {
        if (next == null) return
        val previous = mutableStates.value
        mutableStates.value = next
        val route = when {
            !previous.isAvailable && next.isAvailable -> "/network/available"
            previous.isAvailable && !next.isAvailable -> "/network/lost"
            else -> "/network/changed"
        }
        diagnosticsRepository.recordLocalEvent(route)
    }

    private fun currentRoute(): PlaybackNetworkRoute =
        PlaybackNetworkRoute(
            identity = activeNetwork,
            isBlocked = blocked,
            isValidated = activeCapabilities?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED,
            ),
            isSuspended = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                activeCapabilities?.let {
                    !it.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)
                }
            } else {
                null
            },
        )
}
