package dev.typetype.android.feature.shorts

import android.net.ConnectivityManager
import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortsPrefetchPolicyTest {
    @Test
    fun `api 23 keeps prefetch available without a data saver API`() {
        assertTrue(
            allowsShortsPlaybackPrefetch(
                sdkInt = Build.VERSION_CODES.M,
                restrictBackgroundStatus = ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED,
            ),
        )
    }

    @Test
    fun `prefetch remains available when data saver is disabled`() {
        assertTrue(
            allowsShortsPlaybackPrefetch(
                sdkInt = Build.VERSION_CODES.N,
                restrictBackgroundStatus = ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED,
            ),
        )
    }

    @Test
    fun `prefetch stops when data saver is enabled`() {
        assertFalse(
            allowsShortsPlaybackPrefetch(
                sdkInt = Build.VERSION_CODES.N,
                restrictBackgroundStatus = ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED,
            ),
        )
    }
}
