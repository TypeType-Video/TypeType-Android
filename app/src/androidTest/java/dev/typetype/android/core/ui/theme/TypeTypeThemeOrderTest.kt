package dev.typetype.android.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import dev.typetype.android.domain.preferences.AccentColor
import dev.typetype.android.domain.preferences.AppearanceMode
import dev.typetype.android.domain.preferences.AppearancePersonality
import dev.typetype.android.domain.preferences.AppearanceTheme
import dev.typetype.android.domain.preferences.AppPreferences
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TypeTypeThemeOrderTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val preferences = mutableStateOf(AppPreferences())
    private var scheme: ColorScheme? = null

    @Test
    fun oledKeepsDynamicPaletteAndReplacesOnlySurfaces() {
        preferences.value = AppPreferences(
            accentColor = AccentColor.System,
            appearancePersonality = AppearancePersonality.Classic,
            appearanceMode = AppearanceMode.Dark,
            appearanceTheme = AppearanceTheme.Dynamic,
        )
        composeRule.setContent {
            TypeTypeTheme(preferences = preferences.value) {
                scheme = MaterialTheme.colorScheme
            }
        }
        val dynamic = requireNotNull(scheme)
        val oled = colorSchemeFor(
            preferences.value.copy(appearanceAmoled = true),
        )
        assertEquals(dynamic.primary, oled.primary)
        assertEquals(dynamic.secondaryContainer, oled.secondaryContainer)
        assertEquals(dynamic.tertiary, oled.tertiary)
        assertEquals(dynamic.error, oled.error)
        assertEquals(dynamic.surfaceVariant, oled.surfaceVariant)
        assertEquals(dynamic.outline, oled.outline)
        assertEquals(Color.Black, oled.background)
        assertEquals(Color.Black, oled.surface)
    }

    private fun colorSchemeFor(preferences: AppPreferences): ColorScheme {
        scheme = null
        composeRule.runOnIdle {
            this.preferences.value = preferences
        }
        composeRule.waitForIdle()
        return requireNotNull(scheme)
    }
}
