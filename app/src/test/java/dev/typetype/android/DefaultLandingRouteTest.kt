package dev.typetype.android

import dev.typetype.android.core.ui.navigation.HomeRoute
import dev.typetype.android.core.ui.navigation.LibraryLandingRoute
import dev.typetype.android.core.ui.navigation.LoginRoute
import dev.typetype.android.core.ui.navigation.SubscriptionsRoute
import dev.typetype.android.core.ui.navigation.WelcomeRoute
import dev.typetype.android.domain.auth.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultLandingRouteTest {
    @Test
    fun `maps every frontend landing page to a native destination`() {
        assertEquals(HomeRoute, defaultLandingRoute("home"))
        assertEquals(SubscriptionsRoute, defaultLandingRoute("subscriptions"))
        assertEquals(LibraryLandingRoute("history"), defaultLandingRoute("history"))
        assertEquals(LibraryLandingRoute("playlists"), defaultLandingRoute("playlists"))
        assertEquals(LibraryLandingRoute("watch-later"), defaultLandingRoute("watch-later"))
        assertEquals(LibraryLandingRoute("favorites"), defaultLandingRoute("favorites"))
    }

    @Test
    fun `unknown landing page falls back to home`() {
        assertEquals(HomeRoute, defaultLandingRoute("unknown"))
    }

    @Test
    fun `expired session reauthenticates the selected account`() {
        assertEquals(
            LoginRoute(serverId = "server", accountId = "account"),
            startupRoute("server", "account", SessionStatus.Invalid, "home"),
        )
    }

    @Test
    fun `unknown session keeps cached content available`() {
        assertEquals(
            SubscriptionsRoute,
            startupRoute("server", "account", SessionStatus.Unknown, "subscriptions"),
        )
    }

    @Test
    fun `missing instance returns to setup`() {
        assertEquals(
            WelcomeRoute,
            startupRoute(null, null, SessionStatus.Invalid, "home"),
        )
    }
}
