package dev.typetype.android.feature.player.components

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.services.PlaybackAudioWaveform
import javax.inject.Inject

@OptIn(markerClass = [UnstableApi::class])
@HiltViewModel
class AudioOnlyPosterViewModel @Inject constructor(
    waveform: PlaybackAudioWaveform,
) : ViewModel() {
    val levels = waveform.levels
}
