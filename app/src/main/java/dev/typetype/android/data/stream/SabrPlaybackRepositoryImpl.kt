package dev.typetype.android.data.stream

import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeMediaApi
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackBufferedRange
import dev.typetype.android.domain.stream.SabrPlaybackRepository
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackSnapshot
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class SabrPlaybackRepositoryImpl @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
) : SabrPlaybackRepository {
    private val preparer = SabrPlaybackSessionPreparer()

    override suspend fun prepare(target: SabrPlaybackTarget): Result<SabrPlaybackSession> =
        prepare(target, 0L)

    override suspend fun prepare(
        target: SabrPlaybackTarget,
        startTimeMs: Long,
    ): Result<SabrPlaybackSession> = execute(target) { api ->
        preparer.prepare(api, target.requestScope.baseUrl, target, startTimeMs)
    }

    override suspend fun recoverOnce(
        target: SabrPlaybackTarget,
        startTimeMs: Long,
    ): Result<SabrPlaybackSession> = execute(target) { api ->
        preparer.prepareOnce(api, target.requestScope.baseUrl, target, startTimeMs)
    }

    override suspend fun seek(
        target: SabrPlaybackTarget,
        binding: SabrPlaybackBinding,
        startTimeMs: Long,
    ): Result<SabrPlaybackSession> = execute(target) { api ->
        preparer.seek(api, target.requestScope.baseUrl, target, binding, startTimeMs)
    }

    override suspend fun refresh(
        target: SabrPlaybackTarget,
        binding: SabrPlaybackBinding,
        playerTimeMs: Long,
        bufferedRanges: List<SabrPlaybackBufferedRange>,
        playbackRate: Float,
    ): Result<SabrPlaybackSession> = refresh(target, binding) {
        SabrPlaybackSnapshot(playerTimeMs, bufferedRanges, playbackRate)
    }

    override suspend fun refresh(
        target: SabrPlaybackTarget,
        binding: SabrPlaybackBinding,
        snapshot: () -> SabrPlaybackSnapshot,
    ): Result<SabrPlaybackSession> = execute(target) { api ->
        preparer.refresh(
            api = api,
            baseUrl = target.requestScope.baseUrl,
            target = target,
            binding = binding,
            snapshot = snapshot,
        )
    }

    override suspend fun reportPosition(
        target: SabrPlaybackTarget,
        binding: SabrPlaybackBinding,
        playerTimeMs: Long,
        bufferedRanges: List<SabrPlaybackBufferedRange>,
        playbackRate: Float,
    ): Result<Unit> = execute(target) { api ->
        preparer.reportPosition(
            api = api,
            baseUrl = target.requestScope.baseUrl,
            target = target,
            binding = binding,
            playerTimeMs = playerTimeMs,
            bufferedRanges = bufferedRanges,
            playbackRate = playbackRate,
        )
    }

    private suspend fun <T> execute(
        target: SabrPlaybackTarget,
        request: suspend (TypeTypeMediaApi) -> T,
    ): Result<T> = try {
        target.requireValid()
        val activeScope = activeAccountScope.require()
        check(
            activeScope.serverId == target.requestScope.serverId &&
                activeScope.accountId == target.requestScope.accountId,
        ) {
            "The active account changed before SABR playback control"
        }
        val api = apiHolder.requireSabr(activeScope)
        val result = withContext(Dispatchers.IO) { request(api) }
        activeAccountScope.verify(activeScope)
        Result.success(result)
    } catch (failure: CancellationException) {
        throw failure
    } catch (failure: Throwable) {
        Result.failure(failure)
    }
}

private fun SabrPlaybackTarget.requireValid() {
    require(videoId.isNotBlank() && videoItag > 0 && audioItag > 0 && videoItag != audioItag)
    require(requestScope.serverId.isNotBlank() && requestScope.accountId.isNotBlank())
    require(requestScope.baseUrl.isNotBlank())
}
