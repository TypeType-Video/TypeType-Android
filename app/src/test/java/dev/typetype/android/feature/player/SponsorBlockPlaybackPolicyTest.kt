package dev.typetype.android.feature.player

import dev.typetype.android.domain.stream.SponsorAction
import dev.typetype.android.domain.stream.SponsorBlockSegment
import dev.typetype.android.domain.stream.SponsorCategory
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamPlaybackContract
import dev.typetype.android.domain.usersettings.DEFAULT_SPONSOR_BLOCK_CATEGORY_ACTIONS
import dev.typetype.android.domain.usersettings.SponsorBlockMode
import dev.typetype.android.domain.usersettings.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SponsorBlockPlaybackPolicyTest {
    @Test
    fun `disabled mode hides every segment`() {
        val policy = stream(segment()).sponsorBlockPlaybackPolicy(
            UserSettings(sponsorBlockMode = SponsorBlockMode.Disabled),
        )

        assertTrue(policy.visibleSegments.isEmpty())
        assertTrue(policy.automaticSegments.isEmpty())
        assertTrue(policy.manualSegments.isEmpty())
    }

    @Test
    fun `category and duration settings filter visible segments`() {
        val sponsor = segment()
        val shortIntro = segment(
            startMs = 30_000L,
            endMs = 31_000L,
            category = SponsorCategory.Intro,
        )
        val policy = stream(sponsor, shortIntro).sponsorBlockPlaybackPolicy(
            UserSettings(
                sponsorBlockCategoryActions = DEFAULT_SPONSOR_BLOCK_CATEGORY_ACTIONS +
                    ("sponsor" to SponsorBlockMode.Disabled),
                sponsorBlockMinimumDuration = 2,
            ),
        )

        assertTrue(policy.visibleSegments.isEmpty())
    }

    @Test
    fun `mark only category remains visible without automatic skip`() {
        val highlight = segment(category = SponsorCategory.Poi)
        val policy = stream(highlight).sponsorBlockPlaybackPolicy(UserSettings())

        assertEquals(listOf(highlight), policy.visibleSegments)
        assertTrue(policy.automaticSegments.isEmpty())
    }

    @Test
    fun `mark only global mode disables automatic skip`() {
        val sponsor = segment()
        val policy = stream(sponsor).sponsorBlockPlaybackPolicy(
            UserSettings(sponsorBlockMode = SponsorBlockMode.MarkOnly),
        )

        assertEquals(listOf(sponsor), policy.visibleSegments)
        assertTrue(policy.automaticSegments.isEmpty())
    }

    @Test
    fun `full video segment requires manual skip when configured`() {
        val fullVideo = segment(startMs = 0L, endMs = 95_000L)
        val policy = stream(fullVideo).sponsorBlockPlaybackPolicy(
            UserSettings(sponsorBlockManualSkipOnFullVideo = true),
        )

        assertTrue(policy.automaticSegments.isEmpty())
        assertEquals(listOf(fullVideo), policy.manualSegments)
    }

    @Test
    fun `hidden full video labels remove the segment from playback tools`() {
        val fullVideo = segment(startMs = 0L, endMs = 95_000L)
        val policy = stream(fullVideo).sponsorBlockPlaybackPolicy(
            UserSettings(sponsorBlockShowFullVideoLabels = false),
        )

        assertTrue(policy.visibleSegments.isEmpty())
        assertTrue(policy.automaticSegments.isEmpty())
        assertTrue(policy.manualSegments.isEmpty())
    }

    @Test
    fun `music-only policy skips non-music sections only on music videos`() {
        val musicSegment = segment(category = SponsorCategory.MusicOffTopic)
        val settings = UserSettings(sponsorBlockSkipNonMusicOnlyOnMusicVideos = true)

        val regular = stream(musicSegment).sponsorBlockPlaybackPolicy(settings)
        val music = stream(musicSegment).copy(category = "Music")
            .sponsorBlockPlaybackPolicy(settings)

        assertTrue(regular.automaticSegments.isEmpty())
        assertEquals(listOf(musicSegment), music.automaticSegments)
    }

    @Test
    fun `playback flags follow synchronized settings`() {
        val policy = stream(segment()).sponsorBlockPlaybackPolicy(
            UserSettings(
                sponsorBlockMuteInsteadOfSkip = true,
                sponsorBlockShowCurrentSegment = false,
                sponsorBlockShowChapters = true,
            ),
        )

        assertTrue(policy.muteInsteadOfSkip)
        assertFalse(policy.showCurrentSegment)
        assertTrue(policy.showChapters)
    }

    @Test
    fun `automatic segment only exposes manual skip while muted`() {
        val sponsor = segment()
        val automatic = stream(sponsor).sponsorBlockPlaybackPolicy(UserSettings())
        val muted = stream(sponsor).sponsorBlockPlaybackPolicy(
            UserSettings(sponsorBlockMuteInsteadOfSkip = true),
        )

        assertFalse(automatic.canManuallySkip(sponsor))
        assertTrue(muted.canManuallySkip(sponsor))
    }

    private fun segment(
        startMs: Long = 10_000L,
        endMs: Long = 20_000L,
        category: SponsorCategory = SponsorCategory.Sponsor,
    ) = SponsorBlockSegment(
        startMs = startMs,
        endMs = endMs,
        category = category,
        action = SponsorAction.Skip,
    )

    private fun stream(vararg segments: SponsorBlockSegment) = Stream(
        playbackContract = StreamPlaybackContract.ServerSabr,
        id = "video",
        title = "Video",
        uploaderName = "Channel",
        uploaderAvatarUrl = "",
        uploaderUrl = "",
        uploaderSubscriberCount = 0,
        uploaderVerified = false,
        thumbnailUrl = "",
        description = "",
        durationSeconds = 100,
        viewCount = 0,
        likeCount = 0,
        dislikeCount = 0,
        uploadedAtMillis = 0,
        hlsUrl = null,
        dashMpdUrl = null,
        progressiveUrl = null,
        serverDashManifestUrl = null,
        serverHlsManifestUrl = null,
        startPositionMillis = 0,
        sponsorBlockSegments = segments.toList(),
    )
}
