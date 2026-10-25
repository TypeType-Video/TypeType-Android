package dev.typetype.android.data.stream

import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.StreamResponse
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

internal interface StreamRemoteSource {
    suspend fun load(
        scope: AccountScope,
        videoUrl: String,
        provider: StreamProvider,
        playbackBootstrap: Boolean,
    ): Response<StreamResponse>
}

@Singleton
internal class RetrofitStreamRemoteSource @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
) : StreamRemoteSource {
    override suspend fun load(
        scope: AccountScope,
        videoUrl: String,
        provider: StreamProvider,
        playbackBootstrap: Boolean,
    ): Response<StreamResponse> {
        val api = if (provider == StreamProvider.YouTube) {
            apiHolder.requireSabr(scope)
        } else {
            apiHolder.require(scope)
        }
        return if (provider == StreamProvider.YouTube && playbackBootstrap) {
            api.loadYouTubeSabrBootstrapResponse(videoUrl)
        } else {
            api.loadStreamResponse(videoUrl)
        }
    }
}
