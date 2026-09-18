package video.typetype.tv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.sdk.core.SponsorBlockMode
import video.typetype.sdk.core.SponsorBlockSegment
import video.typetype.sdk.core.StreamDetails
import video.typetype.sdk.core.UserSettings
import video.typetype.sdk.core.VideoId

public class SponsorBlockPolicyTest {
    @Test
    public fun defaultCategoryModesMatchTheFrontendBehavior(): Unit {
        val policy = policy(
            segments = listOf(
                segment(10.0, 20.0, "sponsor"),
                segment(25.0, 30.0, "chapter", action = "chapter"),
            ),
        )

        assertEquals(listOf("sponsor", "chapter"), policy.visibleSegments.map { it.category })
        assertEquals(listOf("sponsor"), policy.autoSkipSegments.map { it.category })
        assertTrue(policy.canManuallySkip(policy.visibleSegments.last()))
    }

    @Test
    public fun normalizesSecondsAndLegacyMilliseconds(): Unit {
        val seconds = policy(listOf(segment(12.5, 18.0, "sponsor")))
        val milliseconds = policy(listOf(segment(12_500.0, 18_000.0, "sponsor")))

        assertEquals(12_500L, seconds.visibleSegments.single().startMilliseconds)
        assertEquals(seconds.visibleSegments, milliseconds.visibleSegments)
    }

    @Test
    public fun fullVideoSegmentsRemainManualWhenRequested(): Unit {
        val policy = policy(
            segments = listOf(segment(0.0, 59.0, "sponsor")),
            settings = UserSettings(sponsorBlockManualSkipOnFullVideo = true),
        )

        assertTrue(policy.autoSkipSegments.isEmpty())
        assertEquals(policy.visibleSegments, policy.manualSkipSegments)
    }

    @Test
    public fun musicOnlyRuleRejectsNonMusicVideos(): Unit {
        val settings = UserSettings(sponsorBlockSkipNonMusicOnlyOnMusicVideos = true)
        val segment = segment(5.0, 15.0, "music_offtopic")

        assertTrue(policy(listOf(segment), settings, category = "Music").autoSkipSegments.isNotEmpty())
        assertTrue(policy(listOf(segment), settings, category = "Entertainment").autoSkipSegments.isEmpty())
    }

    @Test
    public fun minimumDurationAndNearEndTargetAreBounded(): Unit {
        val policy = policy(
            segments = listOf(
                segment(1.0, 2.0, "sponsor"),
                segment(50.0, 60.0, "sponsor"),
            ),
            settings = UserSettings(sponsorBlockMinimumDuration = 3),
        )

        assertEquals(1, policy.visibleSegments.size)
        assertEquals(59_650L, policy.skipTarget(policy.visibleSegments.single(), 60_000L))
        assertFalse(policy.visibleSegments.single().durationMilliseconds < 3_000L)
    }

    private fun policy(
        segments: List<SponsorBlockSegment>,
        settings: UserSettings = UserSettings(),
        category: String = "Entertainment",
    ): SponsorBlockPolicy = SponsorBlockPolicy.create(stream(segments, category), settings)

    private fun stream(segments: List<SponsorBlockSegment>, category: String): StreamDetails = StreamDetails(
        id = VideoId("video"),
        title = "Video",
        uploaderName = "Channel",
        uploaderUrl = "/channel",
        uploaderAvatarUrl = "",
        thumbnailUrl = "",
        description = "",
        durationSeconds = 60,
        viewCount = 0,
        likeCount = 0,
        dislikeCount = 0,
        uploadDate = "",
        uploadedAtEpochSeconds = 0,
        publishedAtEpochSeconds = null,
        streamType = "video",
        isLive = false,
        isPostLive = false,
        isLiveContent = false,
        hasLiveManifest = false,
        isShortFormContent = false,
        originalAudioTrackId = null,
        preferredDefaultAudioTrackId = null,
        videoStreams = emptyList(),
        audioStreams = emptyList(),
        videoOnlyStreams = emptyList(),
        subtitles = emptyList(),
        relatedStreams = emptyList(),
        category = category,
        sponsorBlockSegments = segments,
    )

    private fun segment(
        start: Double,
        end: Double,
        category: String,
        action: String = "skip",
    ): SponsorBlockSegment = SponsorBlockSegment(start, end, category, action)
}
