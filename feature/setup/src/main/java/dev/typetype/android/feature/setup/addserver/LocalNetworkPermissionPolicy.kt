package dev.typetype.android.feature.setup.addserver

import android.Manifest
import android.annotation.SuppressLint

// The permission name is a string inlined at compile time. Below API 37 it is never requested, and checking it
// only reports "denied", which localNetworkPermissionRequired ignores there.
@SuppressLint("InlinedApi")
internal const val LOCAL_NETWORK_PERMISSION = Manifest.permission.ACCESS_LOCAL_NETWORK

internal fun localNetworkPermissionRequired(
    sdkInt: Int,
    permissionGranted: Boolean,
): Boolean = sdkInt >= 37 && !permissionGranted

internal fun isLocalNetworkPermissionPermanentlyDenied(
    canShowRationale: Boolean,
): Boolean = !canShowRationale
