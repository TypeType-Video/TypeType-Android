package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.TwitchDevicePollRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TwitchContractTest {
    private lateinit var server: MockWebServer
    private lateinit var api: TypeTypeApi

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = RetrofitFactory(sessionClient = OkHttpClient(), json = Json { ignoreUnknownKeys = true })
            .create(server.url("/api/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun streamsArePagedWithTheServerParameters() = runBlocking {
        server.enqueue(json("""{"items":[{"id":"9","title":"Spacewalk","user":{"login":"space","displayName":"Space"},"viewers":1084}],"cursor":"c2","hasMore":true}"""))
        val body = api.twitchStreams(after = "c1").body()!!
        assertEquals("/api/twitch/streams?after=c1&first=24", server.takeRequest().path)
        assertEquals("space", body.items.single().user.login)
        assertTrue(body.hasMore)
    }

    @Test
    fun channelLivesVodsAndClipsUseTheLoginInThePath() = runBlocking {
        repeat(3) { server.enqueue(json("""{"items":[]}""")) }
        api.twitchChannelVideos("space")
        api.twitchChannelClips("space", after = "x")
        api.twitchSearchStreams("space walk")
        assertEquals("/api/twitch/channels/space/videos?first=24", server.takeRequest().path)
        assertEquals("/api/twitch/channels/space/clips?after=x&first=24", server.takeRequest().path)
        assertEquals("/api/twitch/search?q=space%20walk&type=streams&first=24", server.takeRequest().path)
    }

    @Test
    fun aLiveCarriesItsRelativePlaylists() = runBlocking {
        server.enqueue(json("""{"channel":{"login":"space","live":{"title":"Live","user":{"login":"space"}}},"hlsUrl":"/live/hls/master.m3u8?c=space","audioUrl":"/live/hls/master.m3u8?a=1&c=space"}"""))
        val live = api.twitchLive("space").body()!!
        assertEquals("/live/hls/master.m3u8?c=space", live.hlsUrl)
        assertEquals("Live", live.channel.live?.title)
    }

    @Test
    fun deviceLoginAndFollowUseTheirMethods() = runBlocking {
        repeat(3) { server.enqueue(json("{}")) }
        api.twitchPollDevice(TwitchDevicePollRequest("code-1"))
        api.twitchFollow("space")
        api.twitchUnfollow("space")
        val poll = server.takeRequest()
        assertEquals("POST /api/twitch/account/poll", "${poll.method} ${poll.path}")
        assertEquals("""{"deviceCode":"code-1"}""", poll.body.readUtf8())
        assertEquals("PUT /api/twitch/me/follows/space", server.takeRequest().let { "${it.method} ${it.path}" })
        assertEquals("DELETE /api/twitch/me/follows/space", server.takeRequest().let { "${it.method} ${it.path}" })
    }

    private fun json(body: String) = MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(body)
}
