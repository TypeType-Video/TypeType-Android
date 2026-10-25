package dev.typetype.android.feature.setup.addserver

import android.Manifest
import android.annotation.SuppressLint

@SuppressLint("InlinedApi")
internal const val LOCAL_NETWORK_PERMISSION = Manifest.permission.ACCESS_LOCAL_NETWORK

internal fun localNetworkPermissionRequired(
    sdkInt: Int,
    permissionGranted: Boolean,
): Boolean = sdkInt >= 37 && !permissionGranted

internal fun isLocalNetworkPermissionPermanentlyDenied(
    canShowRationale: Boolean,
): Boolean = !canShowRationale
