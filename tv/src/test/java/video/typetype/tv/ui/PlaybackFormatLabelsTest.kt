package video.typetype.tv.ui

import org.junit.Assert.assertEquals
import org.junit.Test

public class PlaybackFormatLabelsTest {
    @Test
    public fun technicalCodecProfilesBecomeReadableNames(): Unit {
        assertEquals("AV1", playbackCodecLabel("av01.0.00M.08.0.110.0.05.01.06.0"))
        assertEquals("VP9", playbackCodecLabel("vp09.00.51.08"))
        assertEquals("H.264", playbackCodecLabel("avc1.640028"))
        assertEquals("AAC", playbackCodecLabel("mp4a.40.2"))
        assertEquals("Opus", playbackCodecLabel("opus"))
    }

    @Test
    public fun bitrateIsPresentedInKilobitsPerSecond(): Unit {
        assertEquals("128 kbps", playbackBitrateLabel(128_000L))
        assertEquals("128 kbps", playbackBitrateLabel(128L))
    }
}
