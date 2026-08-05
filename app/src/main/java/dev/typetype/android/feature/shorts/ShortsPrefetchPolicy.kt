package dev.typetype.android.feature.shorts

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build

internal fun Context.allowsShortsMetadataPrefetch(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return true
    val connectivityManager =
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return allowsShortsMetadataPrefetch(
        sdkInt = Build.VERSION.SDK_INT,
        restrictBackgroundStatus = connectivityManager.restrictBackgroundStatus,
    )
}

internal fun allowsShortsMetadataPrefetch(
    sdkInt: Int,
    restrictBackgroundStatus: Int,
): Boolean = sdkInt < Build.VERSION_CODES.N ||
    restrictBackgroundStatus != ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
