package dev.typetype.android.data.network

internal data class PlaybackNetworkRoute(
    val identity: Any?,
    val isBlocked: Boolean,
    val isValidated: Boolean?,
    val isSuspended: Boolean?,
    val isMetered: Boolean = false,
) {
    val isAvailable: Boolean
        get() = identity != null && !isBlocked && isSuspended != true
}

internal class PlaybackNetworkStateTracker(
    initialRoute: PlaybackNetworkRoute,
) {
    private var route = initialRoute

    var state = PlaybackNetworkState(
        isAvailable = initialRoute.isAvailable,
        generation = 0L,
        isMetered = initialRoute.isMetered,
    )
        private set

    fun update(
        nextRoute: PlaybackNetworkRoute,
        routeSignaled: Boolean = false,
    ): PlaybackNetworkState? {
        val previousRoute = route
        route = nextRoute
        val recovered =
            previousRoute.identity == nextRoute.identity &&
                (
                    previousRoute.isValidated == false && nextRoute.isValidated == true ||
                        previousRoute.isSuspended == true && nextRoute.isSuspended == false
                )
        val materiallyChanged =
            routeSignaled ||
                previousRoute.identity != nextRoute.identity ||
                previousRoute.isBlocked != nextRoute.isBlocked ||
                previousRoute.isAvailable != nextRoute.isAvailable ||
                previousRoute.isMetered != nextRoute.isMetered ||
                recovered
        if (!materiallyChanged) return null
        return PlaybackNetworkState(
            isAvailable = nextRoute.isAvailable,
            generation = state.generation + 1L,
            isMetered = nextRoute.isMetered,
        ).also { state = it }
    }
}
