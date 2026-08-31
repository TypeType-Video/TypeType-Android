package video.typetype.tv.ui

import org.junit.Assert.assertEquals
import org.junit.Test

public class CollectionMetadataTest {
    @Test
    fun `unknown counts are not shown`() {
        assertEquals("Rick Astley", collectionMetadata(-1L, "video", "videos", "Rick Astley"))
    }

    @Test
    fun `known counts use the correct label`() {
        assertEquals("1 video · Rick Astley", collectionMetadata(1L, "video", "videos", "Rick Astley"))
        assertEquals("42 videos", collectionMetadata(42L, "video", "videos", ""))
    }
}
