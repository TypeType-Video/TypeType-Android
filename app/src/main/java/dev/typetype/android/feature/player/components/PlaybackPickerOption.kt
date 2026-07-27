package dev.typetype.android.feature.player.components

import dev.typetype.android.feature.player.DecoderSupport

internal data class PlaybackPickerOption(
    val key: String?,
    val label: String,
    val supportingLabel: String? = null,
    val decoderSupport: DecoderSupport? = null,
)
