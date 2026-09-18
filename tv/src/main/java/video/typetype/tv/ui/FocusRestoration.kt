package video.typetype.tv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer

@Composable
internal fun Modifier.restoreFocusWhen(active: Boolean): Modifier {
    val requester = remember { FocusRequester() }
    LaunchedEffect(active) {
        if (active) requester.requestFocus()
    }
    return focusRequester(requester).focusRestorer()
}
