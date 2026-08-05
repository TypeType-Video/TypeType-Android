package dev.typetype.android.data.youtubesession

import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.AccessTokenStore
import dev.typetype.android.data.network.ScopedHttpClientFactory
import dev.typetype.android.domain.server.ServerRepository
import dev.typetype.android.domain.youtubesession.YoutubeRemoteBrowserConnection
import dev.typetype.android.domain.youtubesession.YoutubeRemoteBrowserConnector
import dev.typetype.android.domain.youtubesession.YoutubeRemoteBrowserInput
import dev.typetype.android.domain.youtubesession.YoutubeRemoteBrowserPhase
import dev.typetype.android.domain.youtubesession.YoutubeRemoteBrowserSession
import dev.typetype.android.domain.youtubesession.YoutubeRemoteBrowserState
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

@Singleton
class OkHttpYoutubeRemoteBrowserConnector @Inject constructor(
    private val activeAccountScope: ActiveAccountScope,
    private val serverRepository: ServerRepository,
    private val tokenStore: AccessTokenStore,
    private val clientFactory: ScopedHttpClientFactory,
    private val json: Json,
) : YoutubeRemoteBrowserConnector {
    override suspend fun connect(
        session: YoutubeRemoteBrowserSession,
    ): Result<YoutubeRemoteBrowserConnection> = runCatching {
        val scope = activeAccountScope.require()
        val server = serverRepository.getServer(scope.serverId) ?: error("Instance not found")
        val token = tokenStore.getAccessToken(scope.serverId, scope.accountId)
            ?: error("This account needs to sign in again")
        val url = resolveYoutubeRemoteBrowserUrl(server.baseUrl, session.webSocketUrl)
        activeAccountScope.verify(scope)
        val client = clientFactory.create(
            baseUrl = server.baseUrl,
            serverId = scope.serverId,
            accountId = scope.accountId,
            token = token,
        )
        OkHttpYoutubeRemoteBrowserConnection(
            client = client,
            request = Request.Builder().url(url).build(),
            json = json,
            expectedScope = scope,
            activeAccountScope = activeAccountScope,
        )
    }
}

internal fun resolveYoutubeRemoteBrowserUrl(baseUrl: String, value: String): HttpUrl {
    val base = baseUrl.toHttpUrl()
    val normalized = when {
        value.startsWith("wss://") -> "https://${value.removePrefix("wss://")}".toHttpUrl()
        value.startsWith("ws://") -> "http://${value.removePrefix("ws://")}".toHttpUrl()
        value.startsWith("https://") || value.startsWith("http://") -> value.toHttpUrl()
        else -> base.resolve(value.removePrefix("/")) ?: error("Invalid remote browser URL")
    }
    require(normalized.host == base.host && normalized.port == base.port) {
        "Remote browser origin does not match the instance"
    }
    require(normalized.isHttps == base.isHttps) { "Remote browser transport is not secure" }
    return normalized
}

private class OkHttpYoutubeRemoteBrowserConnection(
    client: OkHttpClient,
    request: Request,
    private val json: Json,
    expectedScope: AccountScope,
    activeAccountScope: ActiveAccountScope,
) : YoutubeRemoteBrowserConnection, WebSocketListener() {
    private val closed = AtomicBoolean(false)
    private val connectionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val frameChannel = Channel<ByteArray>(Channel.CONFLATED)
    private val mutableState = MutableStateFlow(
        YoutubeRemoteBrowserState(YoutubeRemoteBrowserPhase.Connecting),
    )
    private val socket = client.newWebSocket(request, this)

    override val state: StateFlow<YoutubeRemoteBrowserState> = mutableState
    override val frames: Flow<ByteArray> = frameChannel.receiveAsFlow()

    init {
        connectionScope.launch {
            activeAccountScope.observe().collect { current ->
                if (current != expectedScope) close()
            }
        }
    }

    override fun send(input: YoutubeRemoteBrowserInput): Boolean =
        !closed.get() && socket.send(encodeYoutubeRemoteBrowserInput(input))

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        socket.close(NORMAL_CLOSURE_CODE, null)
        frameChannel.close()
        connectionScope.cancel()
        if (mutableState.value.phase !in FINISHED_PHASES) {
            mutableState.value = YoutubeRemoteBrowserState(YoutubeRemoteBrowserPhase.Closed)
        }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        if (text.length > MAX_MESSAGE_CHARACTERS) {
            failAndClose(webSocket)
            return
        }
        parseYoutubeRemoteBrowserMessage(json, text)?.let { mutableState.value = it }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        if (bytes.size > MAX_FRAME_BYTES) {
            failAndClose(webSocket)
            return
        }
        frameChannel.trySend(bytes.toByteArray())
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        finishClosed()
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        if (closed.compareAndSet(false, true)) {
            mutableState.value = YoutubeRemoteBrowserState(YoutubeRemoteBrowserPhase.Error)
            frameChannel.close()
            connectionScope.cancel()
        }
    }

    private fun failAndClose(webSocket: WebSocket) {
        mutableState.value = YoutubeRemoteBrowserState(YoutubeRemoteBrowserPhase.Error)
        webSocket.close(MESSAGE_TOO_LARGE_CODE, null)
    }

    private fun finishClosed() {
        if (!closed.compareAndSet(false, true)) return
        if (mutableState.value.phase !in FINISHED_PHASES) {
            mutableState.value = YoutubeRemoteBrowserState(YoutubeRemoteBrowserPhase.Closed)
        }
        frameChannel.close()
        connectionScope.cancel()
    }

    private companion object {
        const val NORMAL_CLOSURE_CODE = 1000
        const val MESSAGE_TOO_LARGE_CODE = 1009
        const val MAX_MESSAGE_CHARACTERS = 16_384
        const val MAX_FRAME_BYTES = 8 * 1024 * 1024
        val FINISHED_PHASES = setOf(
            YoutubeRemoteBrowserPhase.Connected,
            YoutubeRemoteBrowserPhase.Error,
        )
    }
}
