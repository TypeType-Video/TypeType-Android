package dev.typetype.android.services

import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.binding

internal class SabrPlaybackTransportState(
    initialBinding: SabrPlaybackBinding,
) {
    @Volatile
    private var binding = initialBinding

    fun currentBinding(): SabrPlaybackBinding = binding

    fun accept(session: SabrPlaybackSession) {
        binding = session.binding
    }
}
