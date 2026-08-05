package dev.typetype.android

import dev.typetype.android.core.ui.navigation.HomeRoute
import dev.typetype.android.core.ui.navigation.LibraryLandingRoute
import dev.typetype.android.core.ui.navigation.SubscriptionsRoute
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
}
