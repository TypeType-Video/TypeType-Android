package dev.typetype.android.domain.stream

interface SubtitleRepository {
    suspend fun load(source: StreamSubtitleSource): Result<ByteArray>
}
