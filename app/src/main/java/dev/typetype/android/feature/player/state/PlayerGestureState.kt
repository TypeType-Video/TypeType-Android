package dev.typetype.android.feature.player.state

import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf

enum class GestureSide { Left, Right }
enum class DragMode { None, Brightness, Volume, Seek }

@Stable
class PlayerGestureState {
    val seekHintSide: MutableState<GestureSide?> = mutableStateOf(null)
    val seekHintSeconds = mutableFloatStateOf(0f)
    val brightnessOverlayActive: MutableState<Boolean> = mutableStateOf(false)
    val brightnessFraction = mutableFloatStateOf(0.5f)
    val volumeOverlayActive: MutableState<Boolean> = mutableStateOf(false)
    val volumeFraction = mutableFloatStateOf(0.5f)
    val zoomFillMode: MutableState<Boolean> = mutableStateOf(false)
    val dragMode: MutableState<DragMode> = mutableStateOf(DragMode.None)
    val seekDragStartMs: MutableLongState = mutableLongStateOf(0L)
    val seekDragTargetMs: MutableLongState = mutableLongStateOf(0L)
    val seekDragOverlayActive: MutableState<Boolean> = mutableStateOf(false)
    val longPressBoostActive: MutableState<Boolean> = mutableStateOf(false)
}
