package dev.typetype.android.data.livechat

import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.domain.livechat.LiveChatEvent
import dev.typetype.android.domain.livechat.LiveChatRepository
import dev.typetype.android.domain.livechat.LiveChatRequestException
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
        val response = apiHolder.requireLiveChat(scope).liveChat(videoUrl)
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
                    val parser = LiveChatSseParser(json)
                    val source = it.source()
                    while (true) {
                        val line = source.readUtf8Line() ?: break
                        parser.accept(line.removeSuffix("\r"))?.let { event -> emit(event) }
                    }
                    parser.finish()?.let { event -> emit(event) }
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
