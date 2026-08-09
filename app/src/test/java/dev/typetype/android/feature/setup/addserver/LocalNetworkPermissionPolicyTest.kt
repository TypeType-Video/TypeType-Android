package dev.typetype.android.feature.setup.addserver

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkPermissionPolicyTest {
    @Test
    fun api36ConnectsWithoutRuntimePermission() {
        assertFalse(localNetworkPermissionRequired(sdkInt = 36, permissionGranted = false))
    }

    @Test
    fun api37ConnectsWhenPermissionIsGranted() {
        assertFalse(localNetworkPermissionRequired(sdkInt = 37, permissionGranted = true))
    }

    @Test
    fun api37ResolvesTargetBeforeRequestingPermission() {
        assertTrue(localNetworkPermissionRequired(sdkInt = 37, permissionGranted = false))
    }

    @Test
    fun deniedPermissionBecomesPermanentWhenRationaleCannotBeShown() {
        assertTrue(isLocalNetworkPermissionPermanentlyDenied(canShowRationale = false))
        assertFalse(isLocalNetworkPermissionPermanentlyDenied(canShowRationale = true))
    }
}
