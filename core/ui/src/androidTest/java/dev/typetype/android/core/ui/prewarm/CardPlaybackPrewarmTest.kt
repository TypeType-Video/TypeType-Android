package dev.typetype.android.core.ui.prewarm

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Collections
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardPlaybackPrewarmTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val prewarm = RecordingPrewarm()

    @Test
    fun tappingACardConfirmsItsSession() {
        showCards()

        composeRule.onNodeWithText("video-0").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("confirm:video-0"), prewarm.pressEvents)
    }

    @Test
    fun scrollingPastACardCancelsItsSession() {
        showCards()

        composeRule.onNodeWithTag(LIST_TAG).performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        assertTrue(prewarm.pressEvents.isNotEmpty())
        assertTrue(prewarm.pressEvents.all { it.startsWith("cancel:") })
    }

    @Test
    fun aSettledListPrefetchesOnlyTheMostVisibleStreams() {
        showCards()

        composeRule.waitUntil(timeoutMillis = 5_000) { prewarm.batches.isNotEmpty() }

        assertEquals(listOf(listOf(url(0), url(1))), prewarm.batches.map { batch -> batch.map { it.videoUrl } })
        assertTrue(prewarm.pressEvents.isEmpty())
    }

    private fun showCards() {
        composeRule.setContent {
            val scope = rememberCoroutineScope()
            val settled = remember { SettledCardPrefetch(scope, prewarm) }
            CompositionLocalProvider(
                LocalPlaybackPrewarm provides prewarm,
                LocalSettledCardPrefetch provides settled,
            ) {
                LazyColumn(Modifier.testTag(LIST_TAG)) {
                    items(30) { index ->
                        Box(
                            Modifier
                                .cardPlaybackPrewarm(rememberCardPlaybackPrewarm(url(index), isLive = false))
                                .fillMaxWidth()
                                .height(160.dp)
                                .clickable {},
                        ) {
                            Text("video-$index")
                        }
                    }
                }
            }
        }
    }

    private fun url(index: Int) = "https://www.youtube.com/watch?v=video-$index"

    private class RecordingPrewarm : PlaybackPrewarm {
        val batches: MutableList<List<PrewarmRequest>> = Collections.synchronizedList(mutableListOf())
        val pressEvents: MutableList<String> = Collections.synchronizedList(mutableListOf())

        override fun prefetchStreams(requests: List<PrewarmRequest>) {
            batches += requests
        }

        override fun cancelStreamPrefetch() = Unit

        override fun pressSession(request: PrewarmRequest): PressedSession {
            val id = request.videoUrl.substringAfter("v=")
            return object : PressedSession {
                override fun confirm() {
                    pressEvents += "confirm:$id"
                }

                override fun cancel() {
                    pressEvents += "cancel:$id"
                }
            }
        }
    }

    private companion object {
        const val LIST_TAG = "cards"
    }
}
