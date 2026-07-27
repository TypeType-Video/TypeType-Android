package dev.typetype.android.feature.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.SubtitleParser
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
                WebvttParser().parse(
                    bytes,
                    SubtitleParser.OutputOptions.allCues(),
                    ::add,
                )
            }
        }
}
