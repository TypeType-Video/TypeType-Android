package dev.typetype.android.data.stream

import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.stream.Chapter
import dev.typetype.android.domain.stream.StreamPlaybackContract
import dev.typetype.android.domain.twitch.TwitchClipPlayback
import dev.typetype.android.domain.twitch.TwitchClipQuality
import dev.typetype.android.domain.twitch.TwitchPlayback
import dev.typetype.android.domain.twitch.TwitchTarget
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TwitchStreamLoaderTest {
    @Test
    fun aLivePlaysItsPlaylistAtTheLowLatencyTarget() = runBlocking {
        val loader = TwitchStreamLoader(FakeTwitchRepository(livePlayback = TwitchPlayback(video("https://www.twitch.tv/space"), PLAYLIST, null, live = true)))
        val stream = loader.load(TwitchTarget.Live("space"))
        assertEquals(StreamPlaybackContract.ProviderMedia, stream.playbackContract)
        assertEquals(PLAYLIST, stream.hlsUrl)
        assertTrue(stream.isLive)
        assertEquals(0L, stream.durationSeconds)
        assertEquals(8_000L, stream.liveTargetOffsetMillis)
    }

    @Test
    fun anOfflineChannelFailsWithItsOwnCode() = runBlocking {
        val failure = runCatching { TwitchStreamLoader(FakeTwitchRepository()).load(TwitchTarget.Live("space")) }.exceptionOrNull()
        assertEquals("twitch_offline", (failure as CodedFailure).failureCode)
    }

    @Test
    fun aPastBroadcastKeepsItsLengthAndNoLiveTarget() = runBlocking {
        val loader = TwitchStreamLoader(FakeTwitchRepository(vodPlayback = TwitchPlayback(video("https://www.twitch.tv/videos/1", 3_600), PLAYLIST, null, live = false)))
        val stream = loader.load(TwitchTarget.Vod("1"))
        assertFalse(stream.isLive)
        assertEquals(3_600L, stream.durationSeconds)
        assertNull(stream.liveTargetOffsetMillis)
    }

    @Test
    fun aPastBroadcastCarriesItsGameChaptersOnlyWhenThereAreSeveral() = runBlocking {
        val playback = TwitchPlayback(video("https://www.twitch.tv/videos/1", 3_600), PLAYLIST, null, live = false)
        val two = listOf(Chapter("Fortnite", 0, null), Chapter("Chess", 3_641_000, null))
        assertEquals(two, TwitchStreamLoader(FakeTwitchRepository(vodPlayback = playback, chapterList = two)).load(TwitchTarget.Vod("1")).chapters)
        val one = TwitchStreamLoader(FakeTwitchRepository(vodPlayback = playback, chapterList = two.take(1)))
        assertEquals(emptyList<Chapter>(), one.load(TwitchTarget.Vod("1")).chapters)
    }

    @Test
    fun aClipPlaysTheBestFileUpTo1080p() = runBlocking {
        val qualities = listOf(TwitchClipQuality(1440, 60, "1440"), TwitchClipQuality(1080, 60, "1080"), TwitchClipQuality(360, 30, "360"))
        val loader = TwitchStreamLoader(FakeTwitchRepository(clipPlayback = TwitchClipPlayback(video("https://clips.twitch.tv/a"), qualities)))
        assertEquals("1080", loader.load(TwitchTarget.Clip("a")).progressiveUrl)
    }

    private fun video(url: String, duration: Long = 0) = Video(
        id = url, url = url, title = "Title", thumbnailUrl = "", uploaderName = "Space", uploaderUrl = "https://www.twitch.tv/space",
        uploaderAvatarUrl = "", uploaderVerified = false, durationSeconds = duration, isLive = duration == 0L, viewCount = 1,
        uploadedAtMillis = 1, isShortFormContent = false, shortDescription = null,
    )

    private companion object {
        const val PLAYLIST = "https://watch.example/live/hls/master.m3u8?c=space"
    }
}
