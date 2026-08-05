package dev.typetype.android.services

internal interface PlaybackAudioOnlyController {
    fun setAudioOnly(enabled: Boolean, complete: (Result<Unit>) -> Unit)
}

internal class PlaybackAudioOnlyServiceBridge(
    private val player: androidx.media3.common.Player,
    private val sabr: SabrPlaybackServiceBridge,
    private val provider: ProviderAudioOnlyServiceBridge,
) : PlaybackAudioOnlyController, AutoCloseable {
    override fun setAudioOnly(enabled: Boolean, complete: (Result<Unit>) -> Unit) {
        if (player.currentMediaItem?.sabrPlaybackSeekState() != null) {
            sabr.setAudioOnly(enabled, complete)
        } else {
            provider.setAudioOnly(enabled, complete)
        }
    }

    override fun close() {
        provider.close()
        sabr.close()
    }
}
