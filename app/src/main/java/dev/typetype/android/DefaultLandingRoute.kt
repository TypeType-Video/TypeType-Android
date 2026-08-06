package dev.typetype.android

import dev.typetype.android.core.ui.navigation.HomeRoute
import dev.typetype.android.core.ui.navigation.LibraryLandingRoute
import dev.typetype.android.core.ui.navigation.LoginRoute
import dev.typetype.android.core.ui.navigation.SubscriptionsRoute
import dev.typetype.android.core.ui.navigation.WelcomeRoute
import dev.typetype.android.domain.auth.SessionStatus

internal fun defaultLandingRoute(value: String): Any = when (value) {
    "subscriptions" -> SubscriptionsRoute
    "history", "playlists", "watch-later", "favorites" -> LibraryLandingRoute(value)
    else -> HomeRoute
}

internal fun startupRoute(
    serverId: String?,
    accountId: String?,
    sessionStatus: SessionStatus,
    defaultLandingPage: String,
): Any = when {
    serverId == null -> WelcomeRoute
    sessionStatus == SessionStatus.Invalid -> LoginRoute(serverId, accountId)
    else -> defaultLandingRoute(defaultLandingPage)
}
