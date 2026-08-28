package dev.typetype.android.feature.settings.youtubesession

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.typetype.android.R
import dev.typetype.android.domain.youtubesession.KeyEvent
import dev.typetype.android.domain.youtubesession.PointerEvent
import dev.typetype.android.domain.youtubesession.YoutubeRemoteBrowserInput
import dev.typetype.android.domain.youtubesession.YoutubeRemoteBrowserPhase
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

@Composable
internal fun YoutubeRemoteBrowserPane(
    state: YoutubeSessionState,
    onInput: (YoutubeRemoteBrowserInput) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val browserDescription = stringResource(R.string.youtube_session_browser_accessibility)
    val focusBrowser = stringResource(R.string.youtube_session_browser_focus)
    var typedText by remember(state.remoteSessionId) { mutableStateOf("") }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var frameSize by remember { mutableStateOf(IntSize.Zero) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black, MaterialTheme.shapes.large)
                .onSizeChanged { size ->
                    containerSize = size
                    remoteViewport(size).let { viewport ->
                        onInput(YoutubeRemoteBrowserInput.Resize(viewport.width, viewport.height))
                    }
                }
                .remoteBrowserGestures(
                    containerSize = containerSize,
                    frameSize = frameSize,
                    requestKeyboard = {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    },
                    onInput = onInput,
                )
                .semantics {
                    contentDescription = browserDescription
                    onClick(label = focusBrowser) {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                        true
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            state.frameBytes?.let { bytes ->
                AsyncImage(
                    model = bytes,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    onSuccess = { result ->
                        val size = result.painter.intrinsicSize
                        if (size.width.isFinite() && size.height.isFinite() &&
                            size.width > 0f && size.height > 0f
                        ) {
                            frameSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
                        }
                    },
                )
            } ?: BrowserWaitingState(state)
            BasicTextField(
                value = typedText,
                onValueChange = { candidate ->
                    val next = candidate.take(MAX_TYPED_CHARACTERS)
                    remoteTextEdit(typedText, next).toInputs().forEach(onInput)
                    typedText = next
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(1.dp)
                    .focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.Transparent),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.Transparent),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = {
                        onInput(enterKey(KeyEvent.Down))
                        onInput(enterKey(KeyEvent.Up))
                    },
                ),
                singleLine = true,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(R.string.youtube_session_browser_phase, phaseLabel(state.remotePhase)),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = stringResource(R.string.youtube_session_browser_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.remoteSessionExpiresAt?.let { expiresAt ->
                    Text(
                        text = stringResource(R.string.youtube_session_expires, formatExpiry(expiresAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(onClick = onCancel, enabled = !state.isCancelling) {
                if (state.isCancelling) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.youtube_session_cancel))
                }
            }
        }
    }
}

@Composable
private fun BrowserWaitingState(state: YoutubeSessionState) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (state.remotePhase !in setOf(YoutubeRemoteBrowserPhase.Error, YoutubeRemoteBrowserPhase.Closed)) {
            CircularProgressIndicator(color = Color.White)
        }
        Text(
            text = stringResource(R.string.youtube_session_browser_waiting),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
        )
        state.remoteErrorMessage?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun Modifier.remoteBrowserGestures(
    containerSize: IntSize,
    frameSize: IntSize,
    requestKeyboard: () -> Unit,
    onInput: (YoutubeRemoteBrowserInput) -> Unit,
): Modifier = pointerInput(containerSize, frameSize) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        requestKeyboard()
        var previous = down.position
        var movement = Offset.Zero
        var dragging = false
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            val delta = change.position - previous
            movement += delta
            if (!dragging && movement.getDistance() > viewConfiguration.touchSlop) dragging = true
            if (dragging && delta != Offset.Zero) {
                onInput(YoutubeRemoteBrowserInput.Wheel(-delta.x, -delta.y))
                change.consume()
            }
            previous = change.position
            if (!change.pressed) {
                if (!dragging) {
                    val point = remotePoint(down.position, containerSize, frameSize)
                    onInput(YoutubeRemoteBrowserInput.Pointer(PointerEvent.Down, point.width, point.height))
                    onInput(YoutubeRemoteBrowserInput.Pointer(PointerEvent.Up, point.width, point.height))
                }
                break
            }
        }
    }
}

@Composable
private fun phaseLabel(phase: YoutubeRemoteBrowserPhase): String = stringResource(
    when (phase) {
        YoutubeRemoteBrowserPhase.Idle -> R.string.youtube_session_phase_idle
        YoutubeRemoteBrowserPhase.Connecting -> R.string.youtube_session_phase_connecting
        YoutubeRemoteBrowserPhase.Opening -> R.string.youtube_session_phase_opening
        YoutubeRemoteBrowserPhase.AwaitingLogin -> R.string.youtube_session_phase_awaiting_login
        YoutubeRemoteBrowserPhase.CapturingSession -> R.string.youtube_session_phase_capturing
        YoutubeRemoteBrowserPhase.Connected -> R.string.youtube_session_phase_connected
        YoutubeRemoteBrowserPhase.Closed -> R.string.youtube_session_phase_closed
        YoutubeRemoteBrowserPhase.Error -> R.string.youtube_session_phase_error
    },
)

private fun enterKey(event: KeyEvent) = YoutubeRemoteBrowserInput.Key(
    event = event,
    key = "Enter",
    code = "Enter",
    modifiers = emptyList(),
)

private fun formatExpiry(timestamp: Long): String =
    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(timestamp))

private const val MAX_TYPED_CHARACTERS = 512
