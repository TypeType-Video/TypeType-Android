package dev.typetype.android

import android.app.ActivityManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TypeTypeImageLoaderTest {
    @Test
    fun imageCacheUsesBoundedApplicationMemory() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val memoryClass = context.getSystemService(ActivityManager::class.java).memoryClass
        val loader = createTypeTypeImageLoader(context)

        try {
            assertEquals(
                (memoryClass * 1024L * 1024L * EXPECTED_CACHE_PERCENT).toLong(),
                loader.memoryCache?.maxSize,
            )
        } finally {
            loader.shutdown()
        }
    }
}

private const val EXPECTED_CACHE_PERCENT = 0.10
