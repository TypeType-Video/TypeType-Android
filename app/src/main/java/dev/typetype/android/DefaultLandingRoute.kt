package dev.typetype.android

import dev.typetype.android.core.ui.navigation.HomeRoute
import dev.typetype.android.core.ui.navigation.LibraryLandingRoute
import dev.typetype.android.core.ui.navigation.SubscriptionsRoute

internal fun defaultLandingRoute(value: String): Any = when (value) {
    "subscriptions" -> SubscriptionsRoute
    "history", "playlists", "watch-later", "favorites" -> LibraryLandingRoute(value)
    else -> HomeRoute
}
