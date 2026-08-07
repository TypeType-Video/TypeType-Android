package dev.typetype.android.services

import dev.typetype.android.domain.preferences.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackTaskRemovalPolicyTest {
    private val preferences = MutableStateFlow(AppPreferences())
    private val policy = PlaybackTaskRemovalPolicy(preferences, Dispatchers.Unconfined)

    @Test
    fun `keeps active playback when background pause is disabled`() {
        assertEquals(
            PlaybackTaskRemovalAction.KeepPlaying,
            policy.action(playWhenReady = true, mediaItemCount = 1),
        )
        policy.close()
    }

    @Test
    fun `pauses and stops active playback when background pause is enabled`() {
        preferences.value = AppPreferences(playerPauseInBackground = true)

        assertEquals(
            PlaybackTaskRemovalAction.PauseAndStop,
            policy.action(playWhenReady = true, mediaItemCount = 1),
        )
        policy.close()
    }

    @Test
    fun `stops an inactive service regardless of the preference`() {
        preferences.value = AppPreferences(playerPauseInBackground = true)

        assertEquals(
            PlaybackTaskRemovalAction.Stop,
            policy.action(playWhenReady = false, mediaItemCount = 1),
        )
        assertEquals(
            PlaybackTaskRemovalAction.Stop,
            policy.action(playWhenReady = true, mediaItemCount = 0),
        )
        policy.close()
    }
}
