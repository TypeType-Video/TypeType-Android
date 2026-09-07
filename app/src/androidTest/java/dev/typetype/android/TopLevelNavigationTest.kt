package dev.typetype.android

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.typetype.android.core.ui.navigation.ChannelRoute
import dev.typetype.android.core.ui.navigation.HomeRoute
import dev.typetype.android.core.ui.navigation.LibraryRoute
import dev.typetype.android.core.ui.navigation.SubscriptionsRoute
import org.junit.Rule
import org.junit.Test

class TopLevelNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var controller: NavHostController

    @Test
    fun changingTabsRestoresChannelButReselectingReturnsToSubscriptions() {
        composeRule.setContent {
            controller = rememberNavController()
            NavHost(controller, startDestination = HomeRoute) {
                composable<HomeRoute> { Text("Home") }
                composable<SubscriptionsRoute> { Text("Subscriptions") }
                composable<LibraryRoute> { Text("Library") }
                composable<ChannelRoute> { Text("Channel") }
            }
        }
        composeRule.runOnIdle {
            controller.navigateTopLevel(SubscriptionsRoute, HomeRoute::class.qualifiedName)
        }
        composeRule.runOnIdle { controller.navigate(ChannelRoute("https://www.youtube.com/@channel")) }
        composeRule.onNodeWithText("Channel").assertExists()
        composeRule.runOnIdle {
            controller.navigateTopLevel(LibraryRoute, SubscriptionsRoute::class.qualifiedName)
        }
        composeRule.onNodeWithText("Library").assertExists()
        composeRule.runOnIdle {
            controller.navigateTopLevel(SubscriptionsRoute, LibraryRoute::class.qualifiedName)
        }
        composeRule.onNodeWithText("Channel").assertExists()
        composeRule.runOnIdle {
            controller.navigateTopLevel(SubscriptionsRoute, SubscriptionsRoute::class.qualifiedName)
        }
        composeRule.onNodeWithText("Subscriptions").assertExists()
    }
}
