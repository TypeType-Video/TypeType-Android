package video.typetype.tv.data

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import video.typetype.sdk.core.PlaybackSession
import video.typetype.sdk.core.TypeTypeResult

public fun TvViewModel.startAudioOnlyPlayback() {
    val video = mutableState.value.selectedVideo ?: return
    val stream = mutableState.value.stream ?: return
    viewModelScope.launch {
        mutableState.value = mutableState.value.copy(isLoadingDetails = true, errorMessage = null)
        when (val result = client.catalog.audioOnly(video.url)) {
            is TypeTypeResult.Success -> mutableState.value = mutableState.value.copy(
                audioOnlyStream = result.value,
                playback = PlaybackSession(
                    sessionId = "audio-${stream.id.value}",
                    videoId = stream.id,
                    formats = emptyList(),
                    audioTracks = emptyList(),
                    subtitles = emptyList(),
                    isLive = stream.isLive,
                    ready = true,
                    status = "ready",
                    startTimeMilliseconds = stream.startPositionMilliseconds,
                    durationMilliseconds = result.value.durationSeconds?.times(1_000L)
                        ?: stream.durationSeconds.times(1_000L),
                    transport = "audio-only",
                ),
                isLoadingDetails = false,
            )
            is TypeTypeResult.Failure -> mutableState.value = mutableState.value.copy(
                isLoadingDetails = false,
                errorMessage = result.error.toUserMessage(),
            )
        }
    }
}
