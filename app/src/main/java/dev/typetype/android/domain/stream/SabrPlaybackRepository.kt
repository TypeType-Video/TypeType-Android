package dev.typetype.android.domain.stream

data class SabrPlaybackSelection(
    val video: StreamVideoSource,
    val audio: StreamAudioSource,
    val recoveryVideoItags: Set<Int> = emptySet(),
)

data class SabrPlaybackSession(
    val sessionId: String,
    val manifestUrl: String,
    val generation: Long,
    val videoItag: Int,
    val audioItag: Int,
    val audioTrackId: String?,
    val startTimeMs: Long = 0L,
    val windowEndMs: Long = 30_000L,
    val durationMs: Long = 30_000L,
    val endOfStream: Boolean = false,
    val audioWindow: SabrPlaybackWindowTrack? = null,
    val videoWindow: SabrPlaybackWindowTrack? = null,
)

data class SabrPlaybackBufferedRange(
    val itag: Int,
    val startMs: Long,
    val endMs: Long,
)

data class SabrPlaybackSnapshot(
    val playerTimeMs: Long,
    val bufferedRanges: List<SabrPlaybackBufferedRange>,
    val playbackRate: Float = 1.0f,
)

interface SabrPlaybackRepository {
    suspend fun prepare(target: SabrPlaybackTarget): Result<SabrPlaybackSession>

    suspend fun prepare(target: SabrPlaybackTarget, startTimeMs: Long): Result<SabrPlaybackSession> =
        prepare(target).fold(
            onSuccess = { session ->
                if (startTimeMs <= 0L) {
                    Result.success(session)
                } else {
                    seek(target, session.binding, startTimeMs)
                }
            },
            onFailure = Result.Companion::failure,
        )

    suspend fun recoverOnce(
        target: SabrPlaybackTarget,
        startTimeMs: Long,
    ): Result<SabrPlaybackSession> = prepare(target, startTimeMs)

    suspend fun seek(
        target: SabrPlaybackTarget,
        binding: SabrPlaybackBinding,
        startTimeMs: Long,
    ): Result<SabrPlaybackSession>

    suspend fun refresh(
        target: SabrPlaybackTarget,
        binding: SabrPlaybackBinding,
        playerTimeMs: Long,
        bufferedRanges: List<SabrPlaybackBufferedRange>,
        playbackRate: Float = 1.0f,
    ): Result<SabrPlaybackSession> =
        Result.failure(UnsupportedOperationException("SABR window refresh is not implemented"))

    suspend fun refresh(
        target: SabrPlaybackTarget,
        binding: SabrPlaybackBinding,
        snapshot: () -> SabrPlaybackSnapshot,
    ): Result<SabrPlaybackSession> {
        val current = snapshot()
        return refresh(
            target = target,
            binding = binding,
            playerTimeMs = current.playerTimeMs,
            bufferedRanges = current.bufferedRanges,
            playbackRate = current.playbackRate,
        )
    }

    suspend fun reportPosition(
        target: SabrPlaybackTarget,
        binding: SabrPlaybackBinding,
        playerTimeMs: Long,
        bufferedRanges: List<SabrPlaybackBufferedRange>,
        playbackRate: Float = 1.0f,
    ): Result<Unit> = Result.success(Unit)
}
