package dev.typetype.android.feature.player.components

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.typetype.android.feature.player.state.PlayerGestureState
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerGestureLayerTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun doubleTapZonesSeekTogglePlaybackAndProvideFeedback() {
        val player = GestureTestPlayer(Looper.getMainLooper())
        val feedbackCount = AtomicInteger()
        composeRule.setContent {
            MaterialTheme {
                PlayerGestureLayer(
                    player = player,
                    state = PlayerGestureState(),
                    onSingleTap = {},
                    onAdjustBrightness = {},
                    onAdjustVolume = {},
                    onGestureFeedback = { feedbackCount.incrementAndGet() },
                    config = PlayerGestureConfig(doubleTapSeekSeconds = 5),
                    modifier = Modifier
                        .size(width = 300.dp, height = 180.dp)
                        .testTag(GESTURE_TAG),
                )
            }
        }

        composeRule.onNodeWithTag(GESTURE_TAG).performTouchInput {
            doubleClick(Offset(center.x * 5f / 3f, center.y))
        }
        composeRule.runOnIdle { assertEquals(25_000L, player.currentPosition) }

        composeRule.onNodeWithTag(GESTURE_TAG).performTouchInput {
            doubleClick(Offset(center.x / 3f, center.y))
        }
        composeRule.runOnIdle { assertEquals(20_000L, player.currentPosition) }

        composeRule.onNodeWithTag(GESTURE_TAG).performTouchInput { doubleClick(center) }
        composeRule.runOnIdle {
            assertTrue(player.isPlaying)
            assertEquals(3, feedbackCount.get())
            player.release()
        }
    }

    @Test
    fun enabledHorizontalSwipeSeeksOutsideFullscreen() {
        val player = GestureTestPlayer(Looper.getMainLooper())
        composeRule.setContent {
            MaterialTheme {
                PlayerGestureLayer(
                    player = player,
                    state = PlayerGestureState(),
                    onSingleTap = {},
                    onAdjustBrightness = {},
                    onAdjustVolume = {},
                    config = PlayerGestureConfig(swipeSeekEnabled = true),
                    modifier = Modifier
                        .size(width = 300.dp, height = 180.dp)
                        .testTag(GESTURE_TAG),
                )
            }
        }

        composeRule.onNodeWithTag(GESTURE_TAG).performTouchInput {
            swipe(
                start = Offset(60f, center.y),
                end = Offset(240f, center.y),
                durationMillis = 300L,
            )
        }
        composeRule.runOnIdle {
            assertTrue(player.currentPosition > 20_000L)
            player.release()
        }
    }

    @Test
    fun fullscreenSwipeDownWorksThroughAnInteractiveControl() {
        val exitCount = AtomicInteger()
        val clickCount = AtomicInteger()
        val feedbackCount = AtomicInteger()
        composeRule.setContent {
            MaterialTheme {
                val state = rememberPlayerFullscreenExitGestureState()
                Box(
                    Modifier
                        .size(width = 300.dp, height = 180.dp)
                        .playerFullscreenExitGesture(
                            enabled = true,
                            state = state,
                            onGestureFeedback = { feedbackCount.incrementAndGet() },
                            onExitFullscreen = { exitCount.incrementAndGet() },
                        )
                        .testTag(FULLSCREEN_GESTURE_TAG),
                ) {
                    Button(
                        onClick = { clickCount.incrementAndGet() },
                        modifier = Modifier.align(Alignment.Center)
                            .size(width = 120.dp, height = 64.dp)
                            .testTag(INTERACTIVE_CONTROL_TAG),
                    ) {
                        Text("Play")
                    }
                }
            }
        }

        composeRule.onNodeWithTag(FULLSCREEN_GESTURE_TAG).performTouchInput {
            swipe(
                start = center,
                end = Offset(center.x, center.y + 240f),
                durationMillis = 300L,
            )
        }

        composeRule.runOnIdle {
            assertEquals(1, exitCount.get())
            assertEquals(0, clickCount.get())
            assertEquals(1, feedbackCount.get())
        }
    }

    private companion object {
        const val GESTURE_TAG = "player_gesture_layer"
        const val FULLSCREEN_GESTURE_TAG = "player_fullscreen_exit_gesture"
        const val INTERACTIVE_CONTROL_TAG = "player_interactive_control"
    }
}

private class GestureTestPlayer(looper: Looper) : SimpleBasePlayer(looper) {
    private var positionMs = 20_000L
    private var playWhenReady = false

    override fun getState(): State = State.Builder()
        .setAvailableCommands(Player.Commands.Builder().addAllCommands().build())
        .setPlaybackState(Player.STATE_READY)
        .setPlayWhenReady(
            playWhenReady,
            Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
        )
        .setPlaylist(
            listOf(
                MediaItemData.Builder("gesture-item")
                    .setMediaItem(MediaItem.Builder().setMediaId("gesture-item").build())
                    .build(),
            ),
        )
        .setContentPositionMs(positionMs)
        .build()

    override fun handleSeek(
        mediaItemIndex: Int,
        positionMs: Long,
        seekCommand: Int,
    ): ListenableFuture<*> {
        this.positionMs = positionMs
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        this.playWhenReady = playWhenReady
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleRelease(): ListenableFuture<*> = Futures.immediateVoidFuture()
}
