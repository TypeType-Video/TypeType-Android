package dev.typetype.android

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.typetype.android.core.ui.navigation.AppearanceRoute
import dev.typetype.android.core.ui.navigation.PlayerSettingsRoute
import dev.typetype.android.core.ui.navigation.SettingsRoute
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var controller: NavHostController

    @Test
    fun switchingCategoriesRestoresStateAndDoesNotAccumulateBackEntries() {
        composeRule.setContent {
            controller = rememberNavController()
            NavHost(controller, startDestination = SettingsRoute) {
                composable<SettingsRoute> { Text("Settings index") }
                composable<AppearanceRoute> {
                    var count by rememberSaveable { mutableIntStateOf(0) }
                    Text("Appearance $count", Modifier.clickable { count += 1 })
                }
                composable<PlayerSettingsRoute> { Text("Player settings") }
            }
        }

        composeRule.runOnIdle { controller.selectSettings(AppearanceRoute) }
        composeRule.onNodeWithText("Appearance 0").performClick()
        composeRule.runOnIdle { controller.selectSettings(PlayerSettingsRoute) }
        composeRule.onNodeWithText("Player settings").assertExists()
        composeRule.runOnIdle { controller.selectSettings(AppearanceRoute) }
        composeRule.onNodeWithText("Appearance 1").assertExists()
        composeRule.runOnIdle { controller.selectSettings(AppearanceRoute) }
        composeRule.runOnIdle { assertTrue(controller.popBackStack()) }
        composeRule.onNodeWithText("Settings index").assertExists()
        composeRule.runOnIdle { assertFalse(controller.popBackStack()) }
    }
}
