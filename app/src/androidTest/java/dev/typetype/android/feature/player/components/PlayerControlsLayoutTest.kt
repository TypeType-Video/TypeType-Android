package dev.typetype.android.feature.player.components

import android.os.Looper
import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.ViewCompat
import androidx.core.graphics.Insets
import android.graphics.Bitmap
import java.io.File
import dev.typetype.android.R
import dev.typetype.android.domain.stream.StreamStoryboard
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerControlsLayoutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var controlsView: View

    @Test
    fun portraitControlsDoNotOverlapInsideShortVideoViewport() {
        setControls(360.dp, 202.dp)
        assertControlsDoNotOverlap()
    }

    @Test
    fun landscapePhoneKeepsNormalFullscreenControlSize() {
        setControls(800.dp, 360.dp, smallestWidthDp = 360, fullscreen = true)
        assertControlsDoNotOverlap()
        val height = composeRule.onNodeWithTag(PLAYER_CENTER_CONTROLS_TAG)
            .fetchSemanticsNode().boundsInRoot.height
        assertEquals(with(composeRule.density) { 74.dp.toPx() }, height, 1f)
    }

    @Test
    fun fullscreenTopControlsStayAnchoredWhenStatusBarsDisappear() {
        composeRule.runOnUiThread {
            val window = composeRule.activity.window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, window.decorView)
                .show(WindowInsetsCompat.Type.statusBars())
        }
        setControls(800.dp, 360.dp, fullscreen = true)
        dispatchStatusBarInsets(40)
        composeRule.waitForIdle()
        val visibleTop = backButtonTopInViewport()
        captureControls("status-bars-visible.png")
        dispatchStatusBarInsets(0)
        composeRule.waitForIdle()
        captureControls("status-bars-hidden.png")
        assertEquals(visibleTop, backButtonTopInViewport(), 1f)
    }

    @Test
    fun timelineScrubShowsStoryboardPreviewAndTimecode() {
        val player = controlsLayoutPlayer(
            durationMs = 600_000L,
            positionMs = 120_000L,
        )
        val storyboard = StreamStoryboard(
            urls = listOf("https://example.test/storyboard.jpg"),
            frameWidth = 160,
            frameHeight = 90,
            totalCount = 100,
            durationPerFrameMillis = 5_000L,
            framesPerPageX = 5,
            framesPerPageY = 5,
        )
        composeRule.setContent {
            PlayerTimeBar(
                player = player,
                storyboard = storyboard,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 200.dp),
            )
        }

        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.player_timeline),
        ).performTouchInput {
            down(Offset(width * 0.25f, center.y))
            moveBy(Offset(width * 0.5f, 0f))
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(PLAYER_SEEK_STORYBOARD_PREVIEW_TAG)
            .assertIsDisplayed()
        val previewTexts = composeRule.onNodeWithTag(PLAYER_SEEK_STORYBOARD_PREVIEW_TAG)
            .fetchSemanticsNode().children
            .mapNotNull { child ->
                child.config.getOrElse(SemanticsProperties.Text) { emptyList() }.singleOrNull()
            }
        assertTrue(previewTexts.single().matches(Regex("^\\d{1,2}:\\d{2}$")))
    }

    private fun dispatchStatusBarInsets(top: Int) {
        composeRule.runOnUiThread {
            ViewCompat.dispatchApplyWindowInsets(
                controlsView,
                WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, top, 0, 0))
                    .setVisible(WindowInsetsCompat.Type.statusBars(), top > 0)
                    .build(),
            )
        }
    }

    private fun backButtonTopInViewport(): Float {
        val back = composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.player_back),
        ).fetchSemanticsNode().boundsInRoot
        val viewport = composeRule.onNodeWithTag(PLAYER_CONTROLS_VIEWPORT_TAG)
            .fetchSemanticsNode().boundsInRoot
        return back.top - viewport.top
    }

    private fun captureControls(name: String) {
        val bitmap = composeRule.onNodeWithTag(PLAYER_CONTROLS_VIEWPORT_TAG)
            .captureToImage().asAndroidBitmap()
        File(composeRule.activity.noBackupFilesDir, name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun tabletControlsHaveLargerTargetsWithoutOverlapping() {
        setControls(720.dp, 405.dp, 720)
        assertControlsDoNotOverlap()
        composeRule.onNodeWithTag(PLAYER_CENTER_CONTROLS_TAG)
            .assertHeightIsAtLeast(96.dp)
        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.player_fullscreen),
        ).assertHeightIsAtLeast(64.dp)
        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.player_playback_options),
        ).assertHeightIsAtLeast(64.dp)
    }

    private fun setControls(
        width: Dp,
        height: Dp,
        smallestWidthDp: Int = 360,
        fullscreen: Boolean = false,
    ) {
        val player = controlsLayoutPlayer()
        composeRule.setContent {
            controlsView = LocalView.current
            val configuration = Configuration(LocalConfiguration.current).apply {
                smallestScreenWidthDp = smallestWidthDp
            }
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                Box(
                    Modifier
                        .size(width = width, height = height)
                        .testTag(PLAYER_CONTROLS_VIEWPORT_TAG),
                ) {
                    PlayerControls(
                        player = player,
                        title = "Portrait controls",
                        onNavigateBack = {},
                        isPipAvailable = true,
                        isFullscreen = fullscreen,
                        chaptersAvailable = true,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }
        }
    }

    private fun assertControlsDoNotOverlap() {
        val top = composeRule.onNodeWithTag(PLAYER_TOP_CONTROLS_TAG, useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        val center = composeRule.onNodeWithTag(PLAYER_CENTER_CONTROLS_TAG, useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        val bottom = composeRule.onNodeWithTag(PLAYER_BOTTOM_CONTROLS_TAG, useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        val viewport = composeRule.onNodeWithTag(
            PLAYER_CONTROLS_VIEWPORT_TAG,
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot

        assertTrue("Top controls overlap center controls", top.bottom <= center.top)
        assertTrue("Center controls overlap bottom controls", center.bottom <= bottom.top)
        assertEquals("Portrait controls stop at the viewport bottom", viewport.bottom, bottom.bottom, 1f)
    }
}

private fun controlsLayoutPlayer(
    durationMs: Long = 0L,
    positionMs: Long = 0L,
): Player = Proxy.newProxyInstance(
    Player::class.java.classLoader,
    arrayOf(Player::class.java),
) { _, method, _ ->
    when (method.name) {
        "getApplicationLooper" -> Looper.getMainLooper()
        "getAvailableCommands" -> Player.Commands.EMPTY
        "getDuration" -> durationMs
        "getCurrentPosition" -> positionMs
        "getBufferedPosition" -> positionMs
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
