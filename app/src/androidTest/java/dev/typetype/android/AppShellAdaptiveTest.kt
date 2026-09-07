package dev.typetype.android

import androidx.activity.ComponentActivity
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import androidx.test.espresso.Espresso.closeSoftKeyboard
import dev.typetype.android.core.ui.navigation.HomeRoute
import dev.typetype.android.core.ui.navigation.ChannelRoute
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

    @Test
    fun selectingActiveTabReturnsToTheOriginalTabPage() {
        var navController: NavHostController? = null
        composeRule.setContent {
            val controller = rememberNavController()
            navController = controller
            AppShell(
                navController = controller,
                playerHostController = PlayerHostController(FakePlaybackQueueController()),
                onOpenSearch = {},
                onOpenSettings = {},
                onPlayVideo = {},
                onOpenChannel = {},
                onOpenAccounts = {},
                onClosePlayback = {},
            ) { contentModifier ->
                NavHost(
                    navController = controller,
                    startDestination = HomeRoute,
                    modifier = contentModifier,
                ) {
                    composable<HomeRoute> { androidx.compose.material3.Text("Home content") }
                    composable<SubscriptionsRoute> {
                        androidx.compose.material3.Text("Subscriptions content")
                    }
                    composable<ChannelRoute> {
                        androidx.compose.material3.Text("Channel content")
                    }
                    composable<LibraryRoute> { androidx.compose.material3.Text("Library content") }
                }
            }
        }

        composeRule.onNodeWithText("Subscriptions").performClick()
        composeRule.onNodeWithText("Subscriptions content").assertIsDisplayed()

        composeRule.runOnIdle {
            navController?.navigateToChannel("https://example.com/channel")
        }
        composeRule.onNodeWithText("Channel content").assertIsDisplayed()

        composeRule.onNodeWithText("Subscriptions").performClick()
        composeRule.onNodeWithText("Subscriptions content").assertIsDisplayed()
        composeRule.onNodeWithText("Channel content").assertDoesNotExist()
    }

    @Test
    fun closedKeyboardDoesNotTrapSearchNavigationOrBack() {
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
                    composable<SearchRoute> {
                        var query by rememberSaveable { mutableStateOf("") }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.testTag(SEARCH_FIELD_TAG),
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithContentDescription("Search").performClick()
        composeRule.onNodeWithTag(SEARCH_FIELD_TAG).performTextInput("video")
        closeSoftKeyboard()
        composeRule.onNodeWithText("Library").performClick()
        composeRule.onNodeWithText("Library content").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Search").performClick()
        composeRule.onNodeWithTag(SEARCH_FIELD_TAG).assertTextEquals("")
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.onNodeWithText("Library content").assertIsDisplayed()

        composeRule.onNodeWithText("Home").performClick()
        composeRule.onNodeWithContentDescription("Search").performClick()
        closeSoftKeyboard()
        composeRule.onNodeWithText("Subscriptions").performClick()
        composeRule.onNodeWithText("Subscriptions content").assertIsDisplayed()
        composeRule.onNodeWithTag(SEARCH_FIELD_TAG).assertDoesNotExist()
    }

    @Test
    fun restoredSearchCanLeaveAndReopenWithoutStaleState() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
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
                NavHost(navController, HomeRoute, contentModifier) {
                    composable<HomeRoute> { androidx.compose.material3.Text("Home content") }
                    composable<LibraryRoute> { androidx.compose.material3.Text("Library content") }
                    composable<SearchRoute> {
                        var query by rememberSaveable { mutableStateOf("") }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.testTag(SEARCH_FIELD_TAG),
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithContentDescription("Search").performClick()
        composeRule.onNodeWithTag(SEARCH_FIELD_TAG).performTextInput("video")
        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithTag(SEARCH_FIELD_TAG).assertTextEquals("video")

        composeRule.onNodeWithText("Library").performClick()
        composeRule.onNodeWithText("Library content").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Search").performClick()
        composeRule.onNodeWithTag(SEARCH_FIELD_TAG).assertTextEquals("")
    }

}

private const val SEARCH_FIELD_TAG = "search_field"

private class FakePlaybackQueueController : PlaybackQueueController {
    override val state: StateFlow<PlaybackQueueState> = MutableStateFlow(PlaybackQueueState())
    override fun start(title: String, entries: List<PlaybackQueueEntry>, shuffle: Boolean) = Unit
    override fun restore(snapshot: PlaybackQueueSnapshot) = Unit
    override fun clear() = Unit
}
