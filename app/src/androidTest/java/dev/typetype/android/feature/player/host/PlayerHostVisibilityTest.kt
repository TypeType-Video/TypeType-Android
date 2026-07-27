package dev.typetype.android.feature.player.host

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.unit.dp
import dev.typetype.android.domain.playback.PlaybackQueueController
import dev.typetype.android.domain.playback.PlaybackQueueEntry
import dev.typetype.android.domain.playback.PlaybackQueueSnapshot
import dev.typetype.android.domain.playback.PlaybackQueueState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlayerHostVisibilityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun emptyPlayerCannotCoverContentAfterResize() {
        val landscape = mutableStateOf(false)
        val controller = PlayerHostController(FakePlaybackQueueController())
        composeRule.setContent {
            val modifier = if (landscape.value) {
                Modifier.requiredWidth(800.dp).requiredHeight(400.dp)
            } else {
                Modifier.requiredWidth(400.dp).requiredHeight(800.dp)
            }
            Box(modifier) {
                PlayerHost(
                    controller = controller,
                    bottomBarHeightDp = 0f,
                    isFullscreen = false,
                    onFullscreenChange = {},
                    mediaController = null,
                    onOpenChannel = {},
                    onOpenAccounts = {},
                    onClosePlayback = {},
                    content = {},
                )
            }
        }

        composeRule.runOnIdle {
            landscape.value = true
            controller.expand()
        }
        composeRule.waitForIdle()

        assertEquals(
            0,
            composeRule.onAllNodesWithTag(PLAYER_HOST_OVERLAY_TAG).fetchSemanticsNodes().size,
        )
    }
}

private class FakePlaybackQueueController : PlaybackQueueController {
    override val state: StateFlow<PlaybackQueueState> = MutableStateFlow(PlaybackQueueState())
    override fun start(title: String, entries: List<PlaybackQueueEntry>, shuffle: Boolean) = Unit
    override fun restore(snapshot: PlaybackQueueSnapshot) = Unit
    override fun clear() = Unit
}
