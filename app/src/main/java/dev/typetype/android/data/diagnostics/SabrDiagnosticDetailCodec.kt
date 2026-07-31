package dev.typetype.android.data.diagnostics

import dev.typetype.android.domain.diagnostics.SabrDiagnosticDetail

internal object SabrDiagnosticDetailCodec {
    fun encode(detail: SabrDiagnosticDetail): String = listOf(
        detail.sessionFingerprint,
        detail.generation?.toString().orEmpty(),
        detail.state?.wireValue.orEmpty(),
        detail.track?.wireValue.orEmpty(),
        detail.blocker?.wireValue.orEmpty(),
        detail.terminal?.wireValue.orEmpty(),
        detail.recovery?.wireValue.orEmpty(),
        detail.bufferProgress?.wireValue.orEmpty(),
    ).joinToString(",")

    fun decode(value: String): SabrDiagnosticDetail? {
        val fields = value.split(',', limit = FIELD_COUNT)
        if (fields.size != FIELD_COUNT) return null
        val fingerprint = fields[0].takeIf(FINGERPRINT_PATTERN::matches) ?: return null
        val generation = fields[1].decodeGeneration() ?: fields[1].takeIf(String::isNotEmpty)?.let {
            return null
        }
        val state = fields[2].decodeEnum(
            SabrDiagnosticDetail.State.entries,
            SabrDiagnosticDetail.State::wireValue,
        )
        val track = fields[3].decodeEnum(
            SabrDiagnosticDetail.Track.entries,
            SabrDiagnosticDetail.Track::wireValue,
        )
        val blocker = fields[4].decodeEnum(
            SabrDiagnosticDetail.Blocker.entries,
            SabrDiagnosticDetail.Blocker::wireValue,
        )
        val terminal = fields[5].decodeEnum(
            SabrDiagnosticDetail.Terminal.entries,
            SabrDiagnosticDetail.Terminal::wireValue,
        )
        val recovery = fields[6].decodeEnum(
            SabrDiagnosticDetail.Recovery.entries,
            SabrDiagnosticDetail.Recovery::wireValue,
        )
        val bufferProgress = fields[7].decodeEnum(
            SabrDiagnosticDetail.BufferProgress.entries,
            SabrDiagnosticDetail.BufferProgress::wireValue,
        )
        if (
            fields[2].isUnknown(state) ||
            fields[3].isUnknown(track) ||
            fields[4].isUnknown(blocker) ||
            fields[5].isUnknown(terminal) ||
            fields[6].isUnknown(recovery) ||
            fields[7].isUnknown(bufferProgress)
        ) {
            return null
        }
        return SabrDiagnosticDetail(
            sessionFingerprint = fingerprint,
            generation = generation,
            state = state,
            track = track,
            blocker = blocker,
            terminal = terminal,
            recovery = recovery,
            bufferProgress = bufferProgress,
        )
    }

    private fun String.decodeGeneration(): Long? = takeIf(String::isNotEmpty)
        ?.toLongOrNull()
        ?.takeIf { it >= 0L }

    private fun <T> String.decodeEnum(values: List<T>, wireValue: (T) -> String): T? =
        values.firstOrNull { wireValue(it) == this }

    private fun String.isUnknown(value: Any?): Boolean = isNotEmpty() && value == null

    private const val FIELD_COUNT = 8
    private val FINGERPRINT_PATTERN = Regex("s-[0-9a-f]{12}")
}
