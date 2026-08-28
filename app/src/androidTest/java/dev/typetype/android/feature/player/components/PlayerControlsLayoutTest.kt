package dev.typetype.android.feature.player.components

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.lang.reflect.Proxy
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerControlsLayoutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun portraitControlsDoNotOverlapInsideShortVideoViewport() {
        val player = controlsLayoutPlayer()
        composeRule.setContent {
            Box(Modifier.size(width = 360.dp, height = 202.dp)) {
                PlayerControls(
                    player = player,
                    title = "Portrait controls",
                    onNavigateBack = {},
                    isPipAvailable = true,
                    chaptersAvailable = true,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }

        val top = composeRule.onNodeWithTag(PLAYER_TOP_CONTROLS_TAG, useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        val center = composeRule.onNodeWithTag(PLAYER_CENTER_CONTROLS_TAG, useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        val bottom = composeRule.onNodeWithTag(PLAYER_BOTTOM_CONTROLS_TAG, useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue("Top controls overlap center controls", top.bottom <= center.top)
        assertTrue("Center controls overlap bottom controls", center.bottom <= bottom.top)
    }
}

private fun controlsLayoutPlayer(): Player = Proxy.newProxyInstance(
    Player::class.java.classLoader,
    arrayOf(Player::class.java),
) { _, method, _ ->
    when (method.name) {
        "getApplicationLooper" -> Looper.getMainLooper()
        "getAvailableCommands" -> Player.Commands.EMPTY
        "getCurrentTimeline" -> Timeline.EMPTY
        "getPlaybackParameters" -> PlaybackParameters.DEFAULT
        "getCurrentTracks" -> Tracks.EMPTY
        "getVideoSize" -> VideoSize.UNKNOWN
        else -> when (method.returnType) {
            Boolean::class.javaPrimitiveType -> false
            Int::class.javaPrimitiveType -> 0
            Long::class.javaPrimitiveType -> 0L
            Float::class.javaPrimitiveType -> 0f
            Double::class.javaPrimitiveType -> 0.0
            else -> null
        }
    }
} as Player
