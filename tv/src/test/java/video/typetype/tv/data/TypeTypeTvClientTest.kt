package video.typetype.tv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TypeTypeTvClientTest {
    @Test
    fun instanceKeysNormalizeTheConfiguredUrl() {
        assertEquals("beta_typetype_video_api", instanceKeyFor(" https://beta.typetype.video/api/ "))
    }

    @Test
    fun instanceKeysSeparateHostsAndPaths() {
        assertNotEquals(
            instanceKeyFor("https://typetype.example/api"),
            instanceKeyFor("https://typetype.example/other"),
        )
        assertNotEquals(
            instanceKeyFor("https://beta.typetype.video/api"),
            instanceKeyFor("https://typetype.video/api"),
        )
    }
}
