package dev.typetype.android.feature.player.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.cancel
import androidx.compose.ui.test.centerLeft
import androidx.compose.ui.test.centerRight
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerTimeBarGestureTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun cancelledDragDoesNotCommitASeek() {
        val scrubbed = mutableListOf<Long>()
        val committed = mutableListOf<Long>()
        var cancellations = 0
        showTimeline(scrubbed::add, committed::add) { cancellations++ }

        composeRule.onNodeWithTag(TIMELINE_TAG).performTouchInput {
            down(centerLeft)
            moveTo(centerRight, delayMillis = 200L)
            cancel()
        }

        composeRule.runOnIdle {
            assertTrue(scrubbed.isNotEmpty())
            assertTrue(committed.isEmpty())
            assertEquals(1, cancellations)
        }
    }

    @Test
    fun completedDragCommitsTheLatestPosition() {
        val scrubbed = mutableListOf<Long>()
        val committed = mutableListOf<Long>()
        var cancellations = 0
        showTimeline(scrubbed::add, committed::add) { cancellations++ }

        composeRule.onNodeWithTag(TIMELINE_TAG).performTouchInput {
            down(centerLeft)
            moveTo(centerRight, delayMillis = 200L)
            up()
        }

        composeRule.runOnIdle {
            assertTrue(scrubbed.isNotEmpty())
            assertEquals(scrubbed.last(), committed.single())
            assertEquals(0, cancellations)
        }
    }

    @Test
    fun accessibilityActionCommitsRequestedPosition() {
        val scrubbed = mutableListOf<Long>()
        val committed = mutableListOf<Long>()
        showTimeline(scrubbed::add, committed::add) {}

        composeRule.onNodeWithTag(TIMELINE_TAG).performSemanticsAction(
            SemanticsActions.SetProgress,
        ) { setProgress -> setProgress(45_000f) }

        composeRule.runOnIdle {
            assertEquals(45_000L, scrubbed.single())
            assertEquals(45_000L, committed.single())
        }
    }

    private fun showTimeline(
        onScrub: (Long) -> Unit,
        onScrubFinished: (Long) -> Unit,
        onScrubCancelled: () -> Unit,
    ) {
        composeRule.setContent {
            MaterialTheme {
                TimelineTrack(
                    positionMs = 10_000L,
                    durationMs = 60_000L,
                    segments = emptyList(),
                    compact = false,
                    onScrub = onScrub,
                    onScrubFinished = onScrubFinished,
                    onScrubCancelled = onScrubCancelled,
                    accessibilityLabel = "Playback position",
                    accessibilityStateDescription = "0:10 of 1:00",
                    modifier = Modifier
                        .size(width = 240.dp, height = 36.dp)
                        .testTag(TIMELINE_TAG),
                )
            }
        }
    }

    private companion object {
        const val TIMELINE_TAG = "player_time_bar_timeline"
    }
}
