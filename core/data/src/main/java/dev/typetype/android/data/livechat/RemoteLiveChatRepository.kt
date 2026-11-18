package dev.typetype.android.data.livechat

import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.domain.livechat.LiveChatEvent
import dev.typetype.android.domain.livechat.LiveChatRepository
import dev.typetype.android.domain.livechat.LiveChatRequestException
import dev.typetype.android.domain.twitch.TwitchTarget
import dev.typetype.android.domain.twitch.TwitchUrls
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody

@Singleton
class RemoteLiveChatRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
    private val json: Json,
) : LiveChatRepository {
    override fun connect(videoUrl: String): Flow<LiveChatEvent> = flow {
        val scope = activeAccountScope.require()
        val twitchLogin = (TwitchUrls.parse(videoUrl) as? TwitchTarget.Live)?.login
        val api = apiHolder.requireLiveChat(scope)
        val response = if (twitchLogin != null) api.twitchChatEvents(twitchLogin) else api.liveChat(videoUrl)
        if (!response.isSuccessful) {
            response.errorBody()?.close()
            throw LiveChatRequestException(response.code(), "Live chat request failed (HTTP ${response.code()})")
        }
        val body = response.body() ?: throw LiveChatStreamException("Live chat stream is unavailable")
        body.use {
            if (it.contentType()?.subtype != EVENT_STREAM) throw LiveChatStreamException("Live chat stream is unavailable")
            emit(LiveChatEvent.Connected)
            coroutineScope {
                // A blocked socket read only returns when the server writes, so closing the body is what stops it.
                val closer = launch { closeOnCancel(it) }
                try {
                    val source = it.source()
                    if (twitchLogin != null) {
                        val parser = TwitchChatSseParser(json)
                        while (true) {
                            val line = source.readUtf8Line() ?: break
                            parser.accept(line.removeSuffix("\r"))?.let { event -> emit(event) }
                        }
                    } else {
                        val parser = LiveChatSseParser(json)
                        while (true) {
                            val line = source.readUtf8Line() ?: break
                            parser.accept(line.removeSuffix("\r"))?.let { event -> emit(event) }
                        }
                        parser.finish()?.let { event -> emit(event) }
                    }
                } finally {
                    closer.cancel()
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun closeOnCancel(body: ResponseBody) {
        try {
            awaitCancellation()
        } finally {
            body.close()
        }
    }

    private companion object {
        const val EVENT_STREAM = "event-stream"
    }
}
