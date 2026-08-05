package dev.typetype.android.feature.settings.content

import dev.typetype.android.domain.usersettings.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentSettingsActionTest {
    @Test
    fun `visibility actions only update their server preference`() {
        val original = UserSettings()
        val updated = listOf(
            ContentSettingsAction.SetHideHomeRecommendations(true),
            ContentSettingsAction.SetHideContinueWatching(true),
            ContentSettingsAction.SetHideRelatedVideos(true),
            ContentSettingsAction.SetHideComments(true),
            ContentSettingsAction.SetHideShorts(true),
        ).fold(original) { settings, action -> settings.updatedBy(action) }

        assertTrue(updated.hideHomeRecommendations)
        assertTrue(updated.hideContinueWatching)
        assertTrue(updated.hideRelatedVideos)
        assertTrue(updated.hideComments)
        assertTrue(updated.hideShorts)
        assertFalse(updated.deArrowEnabled)
        assertEquals(original.defaultQuality, updated.defaultQuality)
    }

    @Test
    fun `master visibility action updates every related server preference atomically`() {
        val hidden = UserSettings().updatedBy(ContentSettingsAction.SetAllHidden(true))

        assertTrue(hidden.hideHomeRecommendations)
        assertTrue(hidden.hideContinueWatching)
        assertTrue(hidden.hideRelatedVideos)
        assertTrue(hidden.hideComments)
        assertTrue(hidden.hideShorts)
        assertEquals("home", hidden.defaultLandingPage)
    }

    @Test
    fun `landing page action preserves unrelated preferences`() {
        val updated = UserSettings(autoplay = false).updatedBy(
            ContentSettingsAction.SetDefaultLandingPage("favorites"),
        )

        assertEquals("favorites", updated.defaultLandingPage)
        assertFalse(updated.autoplay)
    }

    @Test
    fun `DeArrow actions preserve the complete frontend preference vocabulary`() {
        val updated = listOf(
            ContentSettingsAction.SetDeArrowEnabled(true),
            ContentSettingsAction.SetDeArrowTitleMode("original"),
            ContentSettingsAction.SetDeArrowThumbnailMode("random"),
            ContentSettingsAction.SetDeArrowTrustMode("locked"),
        ).fold(UserSettings()) { settings, action -> settings.updatedBy(action) }

        assertTrue(updated.deArrowEnabled)
        assertEquals("original", updated.deArrowTitleMode)
        assertEquals("random", updated.deArrowThumbnailMode)
        assertEquals("locked", updated.deArrowTrustMode)
        assertFalse(updated.hideHomeRecommendations)
    }
}
