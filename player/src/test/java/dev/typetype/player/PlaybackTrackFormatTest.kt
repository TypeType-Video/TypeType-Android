package dev.typetype.player

import androidx.media3.common.MimeTypes
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackTrackFormatTest {
    @Test
    fun h264TrackExposesContainerCodecAndSampleMimeType() {
        val format = track(
            PlaybackTrackKind.Video,
            "video/mp4; codecs=\"avc1.640028\"",
        ).toMedia3Format()

        assertEquals(MimeTypes.VIDEO_MP4, format.containerMimeType)
        assertEquals(MimeTypes.VIDEO_H264, format.sampleMimeType)
        assertEquals("avc1.640028", format.codecs)
    }

    @Test
    fun aacTrackExposesMedia3AudioMimeType() {
        val format = track(
            PlaybackTrackKind.Audio,
            "audio/mp4; codecs=\"mp4a.40.2\"",
        ).toMedia3Format()

        assertEquals(MimeTypes.AUDIO_MP4, format.containerMimeType)
        assertEquals(MimeTypes.AUDIO_AAC, format.sampleMimeType)
    }

    @Test
    fun webmTrackUsesTheMatroskaExtractor() {
        val extractor = track(
            PlaybackTrackKind.Video,
            "video/webm; codecs=\"vp09.00.51.08\"",
        ).createExtractor()

        assertTrue(extractor is MatroskaExtractor)
    }

    @Test
    fun mp4TrackUsesTheFragmentedMp4Extractor() {
        val extractor = track(
            PlaybackTrackKind.Video,
            "video/mp4; codecs=\"avc1.640028\"",
        ).createExtractor()

        assertTrue(extractor is FragmentedMp4Extractor)
    }

    private fun track(kind: PlaybackTrackKind, mimeType: String) = PlaybackTrack(
        kind = kind,
        id = if (kind == PlaybackTrackKind.Audio) "140" else "137",
        mimeType = mimeType,
        initializationUrl = "https://example.test/init",
        segments = listOf(
            PlaybackSegment("https://example.test/segment/1", 0L, 1_000_000L),
        ),
    )
}
