package dev.typetype.android.feature.player.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioOnlyDefaultGateTest {
    @Test
    fun `waits until the persisted default is available`() {
        val loading = AudioOnlyDefaultGate(AudioOnlyPlaybackDefault("video-url", enabled = null))
        val loaded = AudioOnlyDefaultGate(AudioOnlyPlaybackDefault("video-url", enabled = true))

        assertFalse(loading.shouldEnable("video-url", available = true, active = false, ready = true))
        assertTrue(loaded.shouldEnable("video-url", available = true, active = false, ready = true))
    }

    @Test
    fun enabledDefaultAppliesOnceToExpectedMedia() {
        val gate = AudioOnlyDefaultGate(AudioOnlyPlaybackDefault("video-url", enabled = true))

        assertFalse(gate.shouldEnable("previous-url", available = true, active = false, ready = true))
        assertTrue(gate.shouldEnable("video-url", available = true, active = false, ready = true))
        assertFalse(gate.shouldEnable("video-url", available = true, active = false, ready = true))
    }

    @Test
    fun unavailableCommandDoesNotConsumeDefault() {
        val gate = AudioOnlyDefaultGate(AudioOnlyPlaybackDefault("video-url", enabled = true))

        assertFalse(gate.shouldEnable("video-url", available = false, active = false, ready = true))
        assertTrue(gate.shouldEnable("video-url", available = true, active = false, ready = true))
    }

    @Test
    fun playbackMustBeReadyBeforeTheDefaultIsConsumed() {
        val gate = AudioOnlyDefaultGate(AudioOnlyPlaybackDefault("video-url", enabled = true))

        assertFalse(gate.shouldEnable("video-url", available = true, active = false, ready = false))
        assertTrue(gate.shouldEnable("video-url", available = true, active = false, ready = true))
    }

    @Test
    fun disabledOrAlreadyActiveDefaultDoesNotSwitchPlayback() {
        val disabled = AudioOnlyDefaultGate(
            AudioOnlyPlaybackDefault("video-url", enabled = false),
        )
        val active = AudioOnlyDefaultGate(AudioOnlyPlaybackDefault("video-url", enabled = true))

        assertFalse(disabled.shouldEnable("video-url", available = true, active = false, ready = true))
        assertFalse(active.shouldEnable("video-url", available = true, active = true, ready = true))
    }
}
