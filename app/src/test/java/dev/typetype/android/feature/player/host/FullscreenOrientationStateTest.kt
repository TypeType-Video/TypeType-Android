package dev.typetype.android.feature.player.host

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FullscreenOrientationStateTest {
    @Test
    fun landscapeRotationEntersFullscreenWithoutLockingOrientation() {
        val transition = FullscreenOrientationState().onEnvironmentChanged(
            orientation = DeviceOrientation.Landscape,
            hasFullscreenMedia = true,
            allowsRotationFullscreen = true,
            isFullscreen = false,
        )

        assertEquals(true, transition.fullscreenRequest)
        assertFalse(transition.state.locksLandscape)
    }

    @Test
    fun portraitRotationExitsRotationDrivenFullscreen() {
        val transition = FullscreenOrientationState().onEnvironmentChanged(
            orientation = DeviceOrientation.Portrait,
            hasFullscreenMedia = true,
            allowsRotationFullscreen = true,
            isFullscreen = true,
        )

        assertEquals(false, transition.fullscreenRequest)
    }

    @Test
    fun fullscreenButtonLocksLandscapeUntilUserExits() {
        val entered = FullscreenOrientationState().onUserRequest(
            fullscreen = true,
            orientation = DeviceOrientation.Portrait,
        )
        val rotated = entered.state.onEnvironmentChanged(
            orientation = DeviceOrientation.Landscape,
            hasFullscreenMedia = true,
            allowsRotationFullscreen = true,
            isFullscreen = true,
        )

        assertTrue(rotated.state.locksLandscape)
        assertNull(rotated.fullscreenRequest)
    }

    @Test
    fun buttonExitDoesNotImmediatelyReenterInLandscape() {
        val exited = FullscreenOrientationState(locksLandscape = true).onUserRequest(
            fullscreen = false,
            orientation = DeviceOrientation.Landscape,
        )
        val environment = exited.state.onEnvironmentChanged(
            orientation = DeviceOrientation.Landscape,
            hasFullscreenMedia = true,
            allowsRotationFullscreen = true,
            isFullscreen = false,
        )

        assertTrue(environment.state.suppressesLandscapeEntry)
        assertNull(environment.fullscreenRequest)
    }

    @Test
    fun portraitClearsLandscapeEntrySuppression() {
        val transition = FullscreenOrientationState(
            suppressesLandscapeEntry = true,
        ).onEnvironmentChanged(
            orientation = DeviceOrientation.Portrait,
            hasFullscreenMedia = true,
            allowsRotationFullscreen = true,
            isFullscreen = false,
        )

        assertFalse(transition.state.suppressesLandscapeEntry)
        assertNull(transition.fullscreenRequest)
    }

    @Test
    fun miniPlayerAndTabletsCannotAutoEnterFullscreen() {
        val transition = FullscreenOrientationState().onEnvironmentChanged(
            orientation = DeviceOrientation.Landscape,
            hasFullscreenMedia = true,
            allowsRotationFullscreen = false,
            isFullscreen = false,
        )

        assertNull(transition.fullscreenRequest)
        assertEquals(FullscreenOrientationState(), transition.state)
    }

    @Test
    fun manualFullscreenRemainsAvailableOnTablets() {
        val transition = FullscreenOrientationState(
            locksLandscape = true,
        ).onEnvironmentChanged(
            orientation = DeviceOrientation.Landscape,
            hasFullscreenMedia = true,
            allowsRotationFullscreen = false,
            isFullscreen = true,
        )

        assertTrue(transition.state.locksLandscape)
        assertNull(transition.fullscreenRequest)
    }

    @Test
    fun losingTheVideoAlwaysExitsFullscreenAndClearsManualLock() {
        val transition = FullscreenOrientationState(
            locksLandscape = true,
        ).onEnvironmentChanged(
            orientation = DeviceOrientation.Landscape,
            hasFullscreenMedia = false,
            allowsRotationFullscreen = false,
            isFullscreen = true,
        )

        assertEquals(false, transition.fullscreenRequest)
        assertEquals(FullscreenOrientationState(), transition.state)
    }

    @Test
    fun savedStateRestoresManualLockAndLandscapeSuppression() {
        val restored = FullscreenOrientationState.Saver.restore(listOf(true, true))

        assertEquals(FullscreenOrientationState(true, true), restored)
    }
}
