package dev.typetype.android.feature.player.components

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.typetype.android.domain.comments.Comment
import dev.typetype.android.domain.comments.CommentImage
import dev.typetype.android.domain.comments.CommentVoiceReply
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommentMediaTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun voiceReplyShowsTranscriptDurationAndReportsAFailedLoad() {
        val requested = mutableListOf<String>()
        composeRule.setContent {
            val playback = rememberVoiceReplyPlayback { url ->
                requested += url
                Result.failure(IllegalStateException("unavailable"))
            }
            CompositionLocalProvider(LocalVoiceReplyPlayback provides playback) {
                CommentMedia(comment(voiceReply = voiceReply(playable = true)))
            }
        }

        composeRule.onNodeWithText("Voice reply").assertExists()
        composeRule.onNodeWithText("Thanks for watching").assertExists()
        composeRule.onNodeWithText("0:13").assertExists()
        composeRule.onNodeWithContentDescription("Play voice reply").performClick()
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodes(hasText("Couldn't play this voice reply")).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(listOf(VOICE_URL), requested)
    }

    @Test
    fun unplayableVoiceReplyShowsItsReasonWithoutAPlayButton() {
        composeRule.setContent {
            CommentMedia(
                comment(voiceReply = voiceReply(playable = false, unavailableText = "Audio is not available")),
            )
        }

        composeRule.onNodeWithText("Audio is not available").assertExists()
        composeRule.onNodeWithContentDescription("Play voice reply").assertDoesNotExist()
    }

    @Test
    fun removedImageShowsTheReasonYoutubeGives() {
        composeRule.setContent {
            CommentMedia(comment(image = CommentImage(url = null, animated = false, unavailableText = "Image removed")))
        }

        composeRule.onNodeWithText("Image removed").assertExists()
    }

    private fun voiceReply(playable: Boolean, unavailableText: String? = null) = CommentVoiceReply(
        url = VOICE_URL,
        durationMs = 12_678,
        transcript = "Thanks for watching",
        playable = playable,
        unavailableText = unavailableText,
    )

    private fun comment(image: CommentImage? = null, voiceReply: CommentVoiceReply? = null) = Comment(
        id = "comment",
        text = "",
        authorName = "Creator",
        authorAvatarUrl = "",
        likeCount = 0,
        textualLikeCount = "",
        publishedTime = "now",
        isHeartedByUploader = false,
        isPinned = false,
        uploaderVerified = true,
        replyCount = 0,
        image = image,
        voiceReply = voiceReply,
    )

    private companion object {
        const val VOICE_URL = "https://www.youtube.com/watch?v=m1"
        const val TIMEOUT_MS = 5_000L
    }
}
