package dev.typetype.android

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.typetype.android.core.ui.navigation.HomeRoute
import dev.typetype.android.core.ui.navigation.LibraryRoute
import dev.typetype.android.core.ui.navigation.SearchRoute
import dev.typetype.android.core.ui.navigation.SubscriptionsRoute
import dev.typetype.android.domain.playback.PlaybackQueueController
import dev.typetype.android.domain.playback.PlaybackQueueEntry
import dev.typetype.android.domain.playback.PlaybackQueueSnapshot
import dev.typetype.android.domain.playback.PlaybackQueueState
import dev.typetype.android.feature.player.host.PlayerHostController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun landscapePhoneUsesBottomNavigation() {
        setShellSize(width = 800.dp, height = 400.dp)

        assertNodeCount(APP_BOTTOM_NAVIGATION_TAG, 1)
        assertNodeCount(APP_NAVIGATION_RAIL_TAG, 0)
    }

    @Test
    fun shortsTabFollowsTheServerVisibilitySetting() {
        val showShorts = mutableStateOf(false)
        setShellSize(width = 300.dp, height = 500.dp, showShorts = showShorts)

        composeRule.onNodeWithText("Shorts").assertDoesNotExist()

        composeRule.runOnIdle { showShorts.value = true }

        composeRule.onNodeWithText("Shorts").assertIsDisplayed()
    }

    @Test
    fun compactNavigationRemainsVisibleAtTwoHundredPercentText() {
        setShellSize(width = 320.dp, height = 500.dp, fontScale = 2f)

        listOf("Home", "Shorts", "Subscriptions", "Library").forEach {
            composeRule.onNodeWithText(it).assertIsDisplayed()
        }
    }

    @Test
    fun rightToLeftLayoutMirrorsTheTopLevelTabs() {
        setShellSize(
            width = 400.dp,
            height = 800.dp,
            layoutDirection = LayoutDirection.Rtl,
        )

        val home = composeRule.onNodeWithText("Home").bounds()
        val library = composeRule.onNodeWithText("Library").bounds()
        assertTrue(home.left > library.left)
    }

    @Test
    fun directionalKeysMoveFocusAcrossTopLevelTabs() {
        setShellSize(width = 400.dp, height = 800.dp, keyboardInput = true)
        val home = composeRule.onNodeWithText("Home")

        home.performSemanticsAction(SemanticsActions.RequestFocus)
        home.assertIsFocused()
        home.performKeyInput { pressKey(Key.DirectionRight) }

        composeRule.onNodeWithText("Shorts").assertIsFocused()
    }

    @Test
    fun topLevelNavigationDoesNotRestoreSearchOverItsOpeningTab() {
        composeRule.setContent {
            val navController = rememberNavController()
            AppShell(
                navController = navController,
                playerHostController = PlayerHostController(FakePlaybackQueueController()),
                onOpenSearch = { navController.navigate(SearchRoute) },
                onOpenSettings = {},
                onPlayVideo = {},
                onOpenChannel = {},
                onOpenAccounts = {},
                onClosePlayback = {},
            ) { contentModifier ->
                NavHost(
                    navController = navController,
                    startDestination = HomeRoute,
                    modifier = contentModifier,
                ) {
                    composable<HomeRoute> { androidx.compose.material3.Text("Home content") }
                    composable<SubscriptionsRoute> {
                        androidx.compose.material3.Text("Subscriptions content")
                    }
                    composable<LibraryRoute> { androidx.compose.material3.Text("Library content") }
                    composable<SearchRoute> { androidx.compose.material3.Text("Search content") }
                }
            }
        }

        composeRule.onNodeWithContentDescription("Search").performClick()
        composeRule.onNodeWithText("Search content").assertIsDisplayed()
        composeRule.onNodeWithText("Subscriptions").performClick()
        composeRule.onNodeWithText("Subscriptions content").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Search").performClick()
        composeRule.onNodeWithText("Search content").assertIsDisplayed()
        composeRule.onNodeWithText("Home").performClick()

        composeRule.onNodeWithText("Home content").assertIsDisplayed()
        composeRule.onNodeWithText("Search content").assertDoesNotExist()
    }

    private fun setShellWidth(width: Dp) {
        setShellSize(width = width, height = 800.dp)
    }

    private fun setShellSize(
        width: Dp,
        height: Dp,
        showShorts: MutableState<Boolean> = mutableStateOf(true),
        fontScale: Float = 1f,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        keyboardInput: Boolean = false,
    ) {
        composeRule.setContent {
            val systemDensity = LocalDensity.current
            val inputModeManager = LocalInputModeManager.current
            LaunchedEffect(keyboardInput) {
                if (keyboardInput) inputModeManager.requestInputMode(InputMode.Keyboard)
            }
            CompositionLocalProvider(
                LocalDensity provides Density(systemDensity.density, fontScale),
                LocalLayoutDirection provides layoutDirection,
            ) {
                val navController = rememberNavController()
                AppShell(
                    navController = navController,
                    playerHostController = PlayerHostController(FakePlaybackQueueController()),
                    onOpenSettings = {},
                    onPlayVideo = {},
                    onOpenChannel = {},
                    onOpenAccounts = {},
                    onClosePlayback = {},
                    showShorts = showShorts.value,
                    modifier = Modifier.requiredWidth(width).requiredHeight(height),
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
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.bounds(): Rect =
        fetchSemanticsNode().boundsInRoot

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
