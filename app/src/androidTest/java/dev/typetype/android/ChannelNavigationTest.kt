package dev.typetype.android

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.typetype.android.core.ui.navigation.ChannelRoute
import dev.typetype.android.core.ui.navigation.HomeRoute
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChannelNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun openingAnotherChannelCreatesStateForItsRoute() {
        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = HomeRoute) {
                composable<HomeRoute> {
                    Button(onClick = { navController.navigateToChannel(FIRST_CHANNEL) }) {
                        Text(OPEN_FIRST)
                    }
                }
                composable<ChannelRoute> {
                    val channelViewModel = viewModel<ChannelRouteTestViewModel>()
                    Column {
                        Text(channelViewModel.channelUrl)
                        Button(onClick = { navController.navigateToChannel(SECOND_CHANNEL) }) {
                            Text(OPEN_SECOND)
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithText(OPEN_FIRST).performClick()
        composeRule.onNodeWithText(FIRST_CHANNEL).assertExists()
        composeRule.onNodeWithText(OPEN_SECOND).performClick()
        composeRule.onNodeWithText(SECOND_CHANNEL).assertExists()
    }

    private companion object {
        const val OPEN_FIRST = "Open first channel"
        const val OPEN_SECOND = "Open second channel"
        const val FIRST_CHANNEL = "https://www.youtube.com/@first"
        const val SECOND_CHANNEL = "https://www.youtube.com/@second"
    }
}

internal class ChannelRouteTestViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val channelUrl = savedStateHandle.toRoute<ChannelRoute>().channelUrl
}
