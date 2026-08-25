package dev.typetype.android.feature.player.state

import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf

enum class GestureSide { Left, Right }
enum class DragMode { None, Brightness, Volume, Seek, FullscreenEnter, FullscreenExit }
enum class ResizeMode { Fit, Crop, Stretch }

fun ResizeMode.next(): ResizeMode = when (this) {
    ResizeMode.Fit -> ResizeMode.Stretch
    ResizeMode.Stretch -> ResizeMode.Crop
    ResizeMode.Crop -> ResizeMode.Fit
}

@Stable
class PlayerGestureState {
    val seekHintSide: MutableState<GestureSide?> = mutableStateOf(null)
    val seekHintSeconds = mutableFloatStateOf(0f)
    val seekHintPulse: MutableLongState = mutableLongStateOf(0L)
    val playbackHintPlaying: MutableState<Boolean?> = mutableStateOf(null)
    val playbackHintPulse: MutableLongState = mutableLongStateOf(0L)
    val brightnessOverlayActive: MutableState<Boolean> = mutableStateOf(false)
    val brightnessFraction = mutableFloatStateOf(0.5f)
    val volumeOverlayActive: MutableState<Boolean> = mutableStateOf(false)
    val volumeFraction = mutableFloatStateOf(0.5f)
    val resizeMode: MutableState<ResizeMode> = mutableStateOf(ResizeMode.Fit)
    val dragMode: MutableState<DragMode> = mutableStateOf(DragMode.None)
    val seekDragStartMs: MutableLongState = mutableLongStateOf(0L)
    val seekDragTargetMs: MutableLongState = mutableLongStateOf(0L)
    val seekDragOverlayActive: MutableState<Boolean> = mutableStateOf(false)
    val longPressBoostActive: MutableState<Boolean> = mutableStateOf(false)

    fun showSeekHint(side: GestureSide, seconds: Int) {
        seekHintSide.value = side
        seekHintSeconds.floatValue = seconds.toFloat()
        seekHintPulse.longValue += 1L
    }

    fun showPlaybackHint(isPlaying: Boolean) {
        playbackHintPlaying.value = isPlaying
        playbackHintPulse.longValue += 1L
    }
}
