package dev.typetype.android

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.typetype.android.core.ui.navigation.HomeRoute
import dev.typetype.android.domain.playback.PlaybackQueueController
import dev.typetype.android.domain.playback.PlaybackQueueEntry
import dev.typetype.android.domain.playback.PlaybackQueueSnapshot
import dev.typetype.android.domain.playback.PlaybackQueueState
import dev.typetype.android.feature.player.host.PlayerHostController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Assert.assertEquals
import org.junit.Test

class AppShellAdaptiveTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun wideWindowUsesNavigationRail() {
        setShellWidth(700.dp)

        assertNodeCount(APP_NAVIGATION_RAIL_TAG, 1)
        assertNodeCount(APP_BOTTOM_NAVIGATION_TAG, 0)
    }

    @Test
    fun compactWindowUsesBottomNavigation() {
        setShellWidth(400.dp)

        assertNodeCount(APP_BOTTOM_NAVIGATION_TAG, 1)
        assertNodeCount(APP_NAVIGATION_RAIL_TAG, 0)
    }

    private fun setShellWidth(width: Dp) {
        composeRule.setContent {
            val navController = rememberNavController()
            AppShell(
                navController = navController,
                playerHostController = PlayerHostController(FakePlaybackQueueController()),
                onOpenSettings = {},
                onPlayVideo = {},
                onOpenChannel = {},
                onOpenAccounts = {},
                onClosePlayback = {},
                modifier = Modifier.requiredWidth(width).requiredHeight(800.dp),
            ) { contentModifier ->
                NavHost(
                    navController = navController,
                    startDestination = HomeRoute,
                    modifier = contentModifier,
                ) {
                    composable<HomeRoute> { }
                }
            }
        }
    }

    private fun assertNodeCount(tag: String, expected: Int) {
        val count = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size
        assertEquals(expected, count)
    }
}

private class FakePlaybackQueueController : PlaybackQueueController {
    override val state: StateFlow<PlaybackQueueState> = MutableStateFlow(PlaybackQueueState())
    override fun start(title: String, entries: List<PlaybackQueueEntry>, shuffle: Boolean) = Unit
    override fun restore(snapshot: PlaybackQueueSnapshot) = Unit
    override fun clear() = Unit
}
