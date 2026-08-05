package dev.typetype.android.feature.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DanmakuSupportTest {
    @Test
    fun acceptsSupportedNicoNicoHosts() {
        assertTrue(supportsServerBulletComments("https://www.nicovideo.jp/watch/sm9"))
        assertTrue(supportsServerBulletComments("https://sp.nicovideo.jp/watch/sm9"))
        assertTrue(supportsServerBulletComments("https://nico.ms/sm9"))
    }

    @Test
    fun rejectsLookalikeAndUnrelatedHosts() {
        assertFalse(supportsServerBulletComments("https://nicovideo.jp.example.com/watch/sm9"))
        assertFalse(supportsServerBulletComments("https://youtube.com/watch?v=sm9"))
        assertFalse(supportsServerBulletComments("not a url"))
    }
}
