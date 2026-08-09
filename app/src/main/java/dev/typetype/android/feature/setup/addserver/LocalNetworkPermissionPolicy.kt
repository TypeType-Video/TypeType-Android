package dev.typetype.android.feature.setup.addserver

internal fun localNetworkPermissionRequired(
    sdkInt: Int,
    permissionGranted: Boolean,
): Boolean = sdkInt >= 37 && !permissionGranted

internal fun isLocalNetworkPermissionPermanentlyDenied(
    canShowRationale: Boolean,
): Boolean = !canShowRationale
