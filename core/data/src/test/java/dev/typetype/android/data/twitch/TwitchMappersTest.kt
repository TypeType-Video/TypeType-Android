package dev.typetype.android.data.twitch

import dev.typetype.android.data.network.dto.TwitchClipQualityDto
import dev.typetype.android.data.network.dto.TwitchStreamDto
import dev.typetype.android.data.network.dto.TwitchUserDto
import dev.typetype.android.data.network.dto.TwitchVideoDto
import dev.typetype.android.domain.twitch.TwitchTarget
import dev.typetype.android.domain.twitch.TwitchUrls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TwitchMappersTest {
    @Test
    fun aLiveBecomesALiveVideoOfItsChannel() {
        val video = TwitchStreamDto(
            title = "Spacewalk",
            user = TwitchUserDto(login = "Space", displayName = "Space Agency", avatarUrl = "https://a.example/a.png"),
            viewers = 1084,
            previewUrl = "https://p.example/p.jpg",
            startedAt = "2026-11-17T18:00:00Z",
        ).toVideo()
        assertEquals("https://www.twitch.tv/space", video.url)
        assertEquals("https://www.twitch.tv/space", video.uploaderUrl)
        assertEquals("Space Agency", video.uploaderName)
        assertTrue(video.isLive)
        assertEquals(1084L, video.viewCount)
        assertEquals(1_794_938_400_000L, video.uploadedAtMillis)
    }

    @Test
    fun aPastBroadcastKeepsItsLengthAndProgressAddress() {
        val video = TwitchVideoDto(id = "2920156347", title = "Walk", owner = TwitchUserDto(login = "space"), lengthSeconds = 33105).toVideo()
        assertEquals("https://www.twitch.tv/videos/2920156347", video.url)
        assertEquals(33105L, video.durationSeconds)
    }

    @Test
    fun playlistsResolveAgainstTheInstanceOrigin() {
        assertEquals(
            "https://watch.example/live/hls/master.m3u8?c=space",
            twitchPlaylistUrl("https://watch.example/api/", "/live/hls/master.m3u8?c=space"),
        )
        assertNull(twitchPlaylistUrl("https://watch.example/api/", " "))
    }

    @Test
    fun clipQualitiesNeedAHeightAndAnAddress() {
        assertEquals(720, TwitchClipQualityDto("720", 29.97, "https://c.example/720.mp4").toQuality()?.height)
        assertNull(TwitchClipQualityDto("source", 30.0, "https://c.example/s.mp4").toQuality())
        assertNull(TwitchClipQualityDto("360", 30.0, "").toQuality())
    }

    @Test
    fun twitchAddressesAreRecognised() {
        assertEquals(TwitchTarget.Live("space"), TwitchUrls.parse("https://www.twitch.tv/Space"))
        assertEquals(TwitchTarget.Vod("1"), TwitchUrls.parse("https://www.twitch.tv/videos/1"))
        assertEquals(TwitchTarget.Clip("Funny-Clip"), TwitchUrls.parse("https://www.twitch.tv/space/clip/Funny-Clip"))
        assertNull(TwitchUrls.parse("https://www.twitch.tv/directory"))
        assertNull(TwitchUrls.parse("https://www.youtube.com/watch?v=a"))
    }

    @Test
    fun badTimesAreIgnored() {
        assertEquals(0L, twitchMillis("soon"))
        assertEquals(1_794_938_400_000L, twitchMillis("2026-11-17T18:00:00.5Z"))
    }
}
