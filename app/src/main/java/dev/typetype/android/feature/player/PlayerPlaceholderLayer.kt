package dev.typetype.android.feature.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics

@Composable
internal fun PlayerPlaceholderLayer(
    hostTransitionProgress: () -> Float,
    content: @Composable () -> Unit,
) {
    val hidden by remember(hostTransitionProgress) {
        derivedStateOf { hostTransitionProgress() >= 0.99f }
    }
    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = (1f - hostTransitionProgress()).coerceIn(0f, 1f) }
            .then(if (hidden) Modifier.clearAndSetSemantics {} else Modifier),
    ) {
        content()
    }
}
