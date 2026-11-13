package dev.typetype.android.feature.player

import dev.typetype.android.domain.preferences.RECOMMENDED_CODEC_KEY
import dev.typetype.android.domain.usersettings.UserSettings
import dev.typetype.android.feature.player.components.PlayerGestureConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPreferenceStateMappingTest {
    @Test
    fun `comment GIFs stay still until the viewer turns them on`() {
        assertFalse(PlayerState().commentGifsAnimated)

        val state = PlayerState().applyPreferences(preferences(commentGifsAnimated = true))

        assertTrue(state.commentGifsAnimated)
        assertFalse(state.applyPreferences(preferences(commentGifsAnimated = false)).commentGifsAnimated)
    }

    private fun preferences(commentGifsAnimated: Boolean) = PlayerPreferenceState(
        gestureConfig = PlayerGestureConfig(),
        brightnessPercent = null,
        autoplayCountdownSeconds = 10,
        audioOnlyPlaybackDefault = false,
        preferredCodec = RECOMMENDED_CODEC_KEY,
        commentGifsAnimated = commentGifsAnimated,
        userSettings = UserSettings(),
    )
}
