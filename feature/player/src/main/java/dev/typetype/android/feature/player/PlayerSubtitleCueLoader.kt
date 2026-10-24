package dev.typetype.android.feature.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.SubtitleParser
import androidx.media3.extractor.text.ttml.TtmlParser
import androidx.media3.extractor.text.webvtt.WebvttParser
import dev.typetype.android.domain.stream.StreamSubtitleSource
import dev.typetype.android.domain.stream.SubtitleRepository
import javax.inject.Inject

typealias LoadSubtitleCues =
    suspend (StreamSubtitleSource) -> Result<List<CuesWithTiming>>

class PlayerSubtitleCueLoader @Inject constructor(
    private val repository: SubtitleRepository,
) {
    @OptIn(UnstableApi::class)
    suspend fun load(source: StreamSubtitleSource): Result<List<CuesWithTiming>> =
        repository.load(source).mapCatching { bytes ->
            buildList {
                source.parser(bytes).parse(
                    bytes,
                    SubtitleParser.OutputOptions.allCues(),
                    ::add,
                )
            }
        }
}

@OptIn(UnstableApi::class)
private fun StreamSubtitleSource.parser(bytes: ByteArray): SubtitleParser =
    if (mimeType.substringBefore(';').trim().equals(WEBVTT_MIME_TYPE, ignoreCase = true) ||
        bytes.decodeToString(throwOnInvalidSequence = false).trimStart().startsWith("WEBVTT")
    ) {
        WebvttParser()
    } else {
        TtmlParser()
    }

private const val WEBVTT_MIME_TYPE = "text/vtt"
