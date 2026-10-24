package dev.typetype.android.core.ui.branding

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import dev.typetype.android.core.ui.components.VideoCard
import dev.typetype.android.domain.branding.DeArrowItem
import dev.typetype.android.domain.branding.DeArrowPreferences
import dev.typetype.android.domain.branding.DeArrowThumbnailCandidate
import dev.typetype.android.domain.branding.DeArrowTitleCandidate
import dev.typetype.android.domain.feed.Video
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DeArrowBrandingEnvironmentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun enabledEnvironmentAppliesServerBranding() {
        val calls = AtomicInteger()
        val environment = DeArrowBrandingEnvironment(
            enabled = true,
            preferences = DeArrowPreferences("dearrow", "dearrow", "accepted"),
            loader = { _, _ ->
                calls.incrementAndGet()
                Result.success(BRANDING)
            },
        )

        composeRule.setContent {
            CompositionLocalProvider(LocalDeArrowBranding provides environment) {
                val branding = rememberVideoBranding(VIDEO_URL, "Original", "original.jpg", 120)
                Text(
                    text = "${branding.title}|${branding.thumbnailUrl}",
                    modifier = Modifier.testTag("branding"),
                )
            }
        }

        composeRule.waitUntil { calls.get() == 1 }
        composeRule.onNodeWithTag("branding").assertTextEquals("Community|community.jpg")
    }

    @Test
    fun disabledEnvironmentKeepsOriginalBrandingWithoutLoading() {
        val calls = AtomicInteger()
        val environment = DeArrowBrandingEnvironment(
            enabled = false,
            preferences = DeArrowPreferences("dearrow", "dearrow", "accepted"),
            loader = { _, _ ->
                calls.incrementAndGet()
                Result.success(BRANDING)
            },
        )

        composeRule.setContent {
            CompositionLocalProvider(LocalDeArrowBranding provides environment) {
                val branding = rememberVideoBranding(VIDEO_URL, "Original", "original.jpg", 120)
                Text(
                    text = "${branding.title}|${branding.thumbnailUrl}",
                    modifier = Modifier.testTag("branding"),
                )
            }
        }

        composeRule.onNodeWithTag("branding").assertTextEquals("Original|original.jpg")
        assertEquals(0, calls.get())
    }

    @Test
    fun deferredEnhancementsKeepOriginalBrandingWithoutLoading() {
        val calls = AtomicInteger()
        val environment = DeArrowBrandingEnvironment(
            enabled = true,
            preferences = DeArrowPreferences("dearrow", "dearrow", "accepted"),
            loader = { _, _ ->
                calls.incrementAndGet()
                Result.success(BRANDING)
            },
        )

        composeRule.setContent {
            CompositionLocalProvider(LocalDeArrowBranding provides environment) {
                val branding = rememberVideoBranding(
                    VIDEO_URL,
                    "Original",
                    "original.jpg",
                    120,
                    loadEnhancements = false,
                )
                Text(
                    text = "${branding.title}|${branding.thumbnailUrl}",
                    modifier = Modifier.testTag("branding"),
                )
            }
        }

        composeRule.onNodeWithTag("branding").assertTextEquals("Original|original.jpg")
        assertEquals(0, calls.get())
    }

    @Test
    fun videoCardRendersResolvedTitle() {
        val environment = DeArrowBrandingEnvironment(
            enabled = true,
            preferences = DeArrowPreferences("dearrow", "dearrow", "accepted"),
            loader = { _, _ -> Result.success(BRANDING) },
        )

        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalDeArrowBranding provides environment) {
                    VideoCard(
                        video = Video(
                            id = "dQw4w9WgXcQ",
                            url = VIDEO_URL,
                            title = "Original",
                            thumbnailUrl = "original.jpg",
                            uploaderName = "Channel",
                            uploaderUrl = "channel",
                            uploaderAvatarUrl = "",
                            uploaderVerified = false,
                            durationSeconds = 120,
                            isLive = false,
                            viewCount = 1,
                            uploadedAtMillis = 1,
                            isShortFormContent = false,
                            shortDescription = null,
                        ),
                    )
                }
            }
        }

        composeRule.onNodeWithText("Community").assertExists()
    }

    private companion object {
        const val VIDEO_URL = "https://youtube.com/watch?v=dQw4w9WgXcQ"
        val BRANDING = DeArrowItem(
            videoId = "dQw4w9WgXcQ",
            legacyTitle = null,
            legacyThumbnailUrl = null,
            titles = listOf(DeArrowTitleCandidate("Community", false, 1, false)),
            thumbnails = listOf(DeArrowThumbnailCandidate("community.jpg", false, 1, false)),
            neutralThumbnailUrl = null,
        )
    }
}
