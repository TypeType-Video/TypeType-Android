package dev.typetype.android.services

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSessionCallbackTest {
    @Test
    fun acceptsTheApplicationController() {
        assertTrue(
            acceptsPlaybackController(
                controllerPackageName = "dev.typetype.android",
                controllerUid = 1001,
                isTrusted = false,
                applicationPackageName = "dev.typetype.android",
                applicationUid = 1001,
            ),
        )
    }

    @Test
    fun acceptsTrustedSystemControllers() {
        assertTrue(
            acceptsPlaybackController(
                controllerPackageName = "android",
                controllerUid = 1000,
                isTrusted = true,
                applicationPackageName = "dev.typetype.android",
                applicationUid = 1001,
            ),
        )
    }

    @Test
    fun rejectsUntrustedExternalControllers() {
        assertFalse(
            acceptsPlaybackController(
                controllerPackageName = "example.other",
                controllerUid = 2002,
                isTrusted = false,
                applicationPackageName = "dev.typetype.android",
                applicationUid = 1001,
            ),
        )
    }

    @Test
    fun manualAudioOnlyChoiceBlocksTheDefaultForTheCurrentMedia() {
        val policy = AudioOnlyDefaultPolicy()

        assertTrue(policy.shouldApplyDefault("first"))
        policy.recordManualChoice("first")

        assertFalse(policy.shouldApplyDefault("first"))
    }

    @Test
    fun replacingTheCurrentMediaClearsTheManualAudioOnlyChoice() {
        val policy = AudioOnlyDefaultPolicy()
        policy.recordManualChoice("first")

        policy.mediaChanged("second")

        assertTrue(policy.shouldApplyDefault("second"))
        assertTrue(policy.shouldApplyDefault("first"))
    }

    @Test
    fun refreshingTheSameMediaKeepsTheManualAudioOnlyChoice() {
        val policy = AudioOnlyDefaultPolicy()
        policy.recordManualChoice("first")

        policy.mediaChanged("first")

        assertFalse(policy.shouldApplyDefault("first"))
    }
}
