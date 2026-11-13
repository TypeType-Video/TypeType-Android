package dev.typetype.android.feature.player.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dev.typetype.android.domain.stream.EndScreenElement
import dev.typetype.android.domain.stream.EndScreenElementType
import dev.typetype.android.feature.player.EndScreenActions
import dev.typetype.android.feature.player.state.ResizeMode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@UnstableApi
class EndScreenOverlayTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var player: ExoPlayer
    private val opened = mutableListOf<String>()
    private var elements by mutableStateOf(emptyList<EndScreenElement>())
    private var enabled by mutableStateOf(true)
    private val actions = EndScreenActions(
        onOpenVideo = { opened += "video:$it" },
        onOpenChannel = { opened += "channel:$it" },
        onOpenPlaylist = { opened += "playlist:$it" },
    )

    @Before
    fun setUp() {
        composeRule.runOnIdle { player = ExoPlayer.Builder(composeRule.activity).build() }
    }

    @After
    fun tearDown() {
        composeRule.runOnIdle { player.release() }
    }

    @Test
    fun placesEachElementInsideTheVideoFrameAndOpensIt() {
        show(
            element("video", EndScreenElementType.Video, left = 0.5f, top = 0.25f, width = 0.4f),
            element("playlist", EndScreenElementType.Playlist, left = 0.05f, top = 0.25f, width = 0.4f),
            element("channel", EndScreenElementType.Channel, left = 0.4f, top = 0.7f, width = 0.1f, aspectRatio = 1f),
        )

        val video = composeRule.onNodeWithContentDescription("video title").assertIsDisplayed()
        val bounds = video.getUnclippedBoundsInRoot()
        assertEquals(160f, bounds.left.value, 1f)
        assertEquals(45f, bounds.top.value, 1f)
        assertEquals(128f, bounds.width.value, 1f)
        assertEquals(72f, bounds.height.value, 1f)
        val channel = composeRule.onNodeWithContentDescription("channel title").getUnclippedBoundsInRoot()
        assertEquals(channel.width.value, channel.height.value, 1f)

        video.performClick()
        composeRule.onNodeWithContentDescription("playlist title").performClick()
        composeRule.onNodeWithContentDescription("channel title").performClick()

        composeRule.runOnIdle {
            assertEquals(listOf("video:url-video", "playlist:url-playlist", "channel:url-channel"), opened)
        }
    }

    @Test
    fun asksBeforeLeavingTheAppForAWebsite() {
        show(element("site", EndScreenElementType.Website, left = 0.4f, top = 0.4f, width = 0.1f, aspectRatio = 1f))

        composeRule.onNodeWithContentDescription("site title").performClick()

        composeRule.onNodeWithText("Leaving TypeType").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(emptyList<String>(), opened) }
    }

    @Test
    fun showsNothingOutsideTheElementWindowOrWhenDisabled() {
        show(element("later", EndScreenElementType.Video, startMs = 60_000, endMs = 80_000))
        composeRule.onAllNodesWithContentDescription("later title").assertCountEquals(0)

        composeRule.runOnIdle {
            enabled = false
            elements = listOf(element("now", EndScreenElementType.Video))
        }
        composeRule.onAllNodesWithContentDescription("now title").assertCountEquals(0)

        composeRule.runOnIdle { enabled = true }
        composeRule.onNodeWithContentDescription("now title").assertIsDisplayed()
    }

    private fun show(vararg shown: EndScreenElement) {
        elements = shown.toList()
        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    EndScreenOverlay(
                        player = player,
                        elements = elements,
                        resizeMode = ResizeMode.Fit,
                        enabled = enabled,
                        actions = actions,
                        modifier = Modifier.size(width = 320.dp, height = 180.dp),
                    )
                }
            }
        }
    }

    private fun element(
        id: String,
        type: EndScreenElementType,
        startMs: Long = 0,
        endMs: Long = 20_000,
        left: Float = 0.1f,
        top: Float = 0.1f,
        width: Float = 0.3f,
        aspectRatio: Float = 16f / 9f,
    ) = EndScreenElement(id, type, "$id title", "url-$id", "", startMs, endMs, left, top, width, aspectRatio)
}
