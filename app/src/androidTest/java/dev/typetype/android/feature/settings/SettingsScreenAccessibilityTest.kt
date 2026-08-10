package dev.typetype.android.feature.settings

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dev.typetype.android.R
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenAccessibilityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun everySettingsDestinationRemainsReachableAtTwoHundredPercentText() {
        showSettings(fontScale = 2f)

        destinationLabels().forEach { label ->
            composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(label))
            composeRule.onNodeWithText(label).assertIsDisplayed()
        }
    }

    @Test
    fun everyInteractiveControlHasSpokenText() {
        showSettings()

        composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
            val text = node.config.getOrNull(SemanticsProperties.Text).orEmpty()
            val description = node.config
                .getOrNull(SemanticsProperties.ContentDescription)
                .orEmpty()
            assertTrue(
                "Interactive settings control has no accessible label: ${node.config}",
                text.isNotEmpty() || description.isNotEmpty(),
            )
        }
    }

    @Test
    fun unsupportedServerFeaturesStayHidden() {
        composeRule.setContent {
            TypeTypeTheme {
                SettingsScreen(
                    onNavigateBack = {},
                    onOpenAppearance = {},
                    onSignOut = {},
                )
            }
        }

        composeRule.onNodeWithText(resource(R.string.youtube_session_settings_title))
            .assertDoesNotExist()
        composeRule.onNodeWithText(resource(R.string.rss_settings_title)).assertDoesNotExist()
    }

    @Test
    fun keyboardCanActivateASettingsDestinationInRightToLeftLayout() {
        val opened = AtomicBoolean()
        showSettings(
            layoutDirection = LayoutDirection.Rtl,
            keyboardInput = true,
            onOpenAccounts = opened::set,
        )
        val accounts = composeRule.onNodeWithText(resource(R.string.accounts_title))

        accounts.performSemanticsAction(SemanticsActions.RequestFocus)
        accounts.assertIsFocused()
        accounts.performKeyInput { pressKey(Key.Enter) }

        assertTrue(opened.get())
    }

    private fun showSettings(
        fontScale: Float = 1f,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        keyboardInput: Boolean = false,
        onOpenAccounts: (Boolean) -> Unit = {},
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
                TypeTypeTheme {
                    SettingsScreen(
                        onNavigateBack = {},
                        onOpenProfile = {},
                        onOpenImport = {},
                        importsAvailable = true,
                        onOpenYoutubeSession = {},
                        youtubeSessionAvailable = true,
                        onOpenRssFeeds = {},
                        rssAvailable = true,
                        onOpenAccounts = { onOpenAccounts(true) },
                        onOpenAppearance = {},
                        onOpenContent = {},
                        onOpenPlayer = {},
                        onOpenStorage = {},
                        onOpenPrivacy = {},
                        onOpenDiagnostics = {},
                        onOpenBlocked = {},
                        onOpenAbout = {},
                        onSignOut = {},
                    )
                }
            }
        }
    }

    private fun destinationLabels(): List<String> = listOf(
        R.string.accounts_title,
        R.string.settings_profile_title,
        R.string.settings_import_title,
        R.string.youtube_session_settings_title,
        R.string.rss_settings_title,
        R.string.settings_appearance_title,
        R.string.settings_content_title,
        R.string.settings_player_title,
        R.string.settings_storage_title,
        R.string.settings_privacy_title,
        R.string.diagnostics_title,
        R.string.settings_blocked_title,
        R.string.settings_about_title,
        R.string.settings_sign_out,
    ).map(::resource)

    private fun resource(id: Int): String = composeRule.activity.getString(id)
}
