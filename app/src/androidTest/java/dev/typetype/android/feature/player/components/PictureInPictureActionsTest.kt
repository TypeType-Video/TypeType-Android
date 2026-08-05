package dev.typetype.android.feature.player.components

import android.content.ComponentName
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.filters.SdkSuppress
import dev.typetype.android.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class PictureInPictureActionsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun pictureInPictureExposesPlaybackAndAudioActions() {
        composeRule.activityRule.scenario.onActivity { activity ->
            val params = PictureInPictureApi26.buildParams(
                activity = activity,
                aspectRatio = Rational(16, 9),
                autoEnter = false,
                isPlaying = true,
                audioOnlyAvailable = true,
                sourceRect = null,
            )

            val actions = params.actions
            assertEquals(2, actions.size)
            assertEquals(activity.getString(R.string.player_action_pause), actions[0].title)
            assertEquals(activity.getString(R.string.player_audio_only), actions[1].title)

            val pausedParams = PictureInPictureApi26.buildParams(
                activity = activity,
                aspectRatio = Rational(16, 9),
                autoEnter = false,
                isPlaying = false,
                audioOnlyAvailable = true,
                sourceRect = null,
            )
            assertEquals(activity.getString(R.string.player_action_play), pausedParams.actions[0].title)

            val liveParams = PictureInPictureApi26.buildParams(
                activity = activity,
                aspectRatio = Rational(16, 9),
                autoEnter = false,
                isPlaying = true,
                audioOnlyAvailable = false,
                sourceRect = null,
            )
            assertEquals(1, liveParams.actions.size)
            assertEquals(activity.getString(R.string.player_action_pause), liveParams.actions[0].title)

            val receiver = activity.packageManager.getReceiverInfo(
                ComponentName(activity, PictureInPictureActionReceiver::class.java),
                0,
            )
            assertFalse(receiver.exported)
        }
    }
}
