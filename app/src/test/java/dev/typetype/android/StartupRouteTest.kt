package dev.typetype.android

import dev.typetype.android.core.ui.navigation.HomeRoute
import dev.typetype.android.core.ui.navigation.LibraryLandingRoute
import dev.typetype.android.core.ui.navigation.LoginRoute
import dev.typetype.android.core.ui.navigation.SubscriptionsRoute
import dev.typetype.android.core.ui.navigation.WelcomeRoute
import dev.typetype.android.domain.auth.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class StartupRouteTest {
    @Test
    fun `missing server starts welcome`() {
        assertEquals(
            WelcomeRoute,
            startupRoute(
                serverId = null,
                accountId = null,
                sessionStatus = SessionStatus.Invalid,
                defaultLandingPage = "",
            ),
        )
    }

    @Test
    fun `invalid session starts login and preserves the selected account`() {
        assertEquals(
            LoginRoute("server", "account"),
            startupRoute(
                serverId = "server",
                accountId = "account",
                sessionStatus = SessionStatus.Invalid,
                defaultLandingPage = "",
            ),
        )
    }

    @Test
    fun `known session starts the cached subscription landing page`() {
        assertEquals(
            SubscriptionsRoute,
            startupRoute(
                serverId = "server",
                accountId = "account",
                sessionStatus = SessionStatus.Unknown,
                defaultLandingPage = "subscriptions",
            ),
        )
    }

    @Test
    fun `known session starts the cached library landing page`() {
        assertEquals(
            LibraryLandingRoute("watch-later"),
            startupRoute(
                serverId = "server",
                accountId = "account",
                sessionStatus = SessionStatus.Valid,
                defaultLandingPage = "watch-later",
            ),
        )
    }

    @Test
    fun `unknown landing page starts home`() {
        assertEquals(
            HomeRoute,
            startupRoute(
                serverId = "server",
                accountId = "account",
                sessionStatus = SessionStatus.Valid,
                defaultLandingPage = "",
            ),
        )
    }
}
