package dev.typetype.android.data.library

import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.network.dto.PlaylistDto
import dev.typetype.android.data.network.dto.PlaylistVideoDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistResponseMapperTest {
    @Test
    fun `summary preserves authoritative server count without videos`() {
        val entity = playlist(videoCount = 12).toPlaylistEntity(SCOPE)

        assertEquals(12, entity.videoCount)
        assertTrue(playlist(videoCount = 12).toVideoEntities(SCOPE).isEmpty())
    }

    @Test
    fun `detail deduplicates repeated video urls`() {
        val duplicate = video(id = "second")
        val dto = playlist(
            videoCount = 2,
            videos = listOf(video(id = "first"), duplicate),
        )

        val videos = dto.toVideoEntities(SCOPE)

        assertEquals(1, videos.size)
        assertEquals("first", videos.single().id)
        assertEquals(2, dto.toPlaylistEntity(SCOPE).videoCount)
    }

    private fun playlist(
        videoCount: Int,
        videos: List<PlaylistVideoDto> = emptyList(),
    ) = PlaylistDto(
        id = "saved",
        name = "Saved",
        videos = videos,
        videoCount = videoCount,
        createdAt = 1L,
    )

    private fun video(id: String) = PlaylistVideoDto(
        id = id,
        url = "youtube-video",
        title = "Video",
        thumbnail = "thumbnail",
        duration = 42L,
        position = 0,
    )

    private companion object {
        val SCOPE = AccountScope("server", "account")
    }
}
