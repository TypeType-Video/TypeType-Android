package dev.typetype.android.feature.player.components

import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.filters.SdkSuppress
import dev.typetype.android.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class PictureInPictureActionsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun playbackControlsRemainOwnedByTheMediaSession() {
        composeRule.activityRule.scenario.onActivity { activity ->
            val params = PictureInPictureApi26.buildParams(
                activity = activity,
                aspectRatio = Rational(16, 9),
                autoEnter = false,
                isPlaying = true,
                sourceRect = null,
            )

            val actions = params.actions
            assertEquals(1, actions.size)
            assertEquals(activity.getString(R.string.player_audio_only), actions.single().title)
        }
    }
}
