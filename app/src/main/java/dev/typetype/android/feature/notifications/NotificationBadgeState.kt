package dev.typetype.android.feature.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun rememberNotificationBadge(
    viewModel: NotificationsBadgeViewModel = hiltViewModel(),
): State<dev.typetype.android.domain.notifications.NotificationBadge> {
    val badge = viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner by rememberUpdatedState(LocalLifecycleOwner.current)
    androidx.compose.runtime.LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (currentCoroutineContext().isActive) {
                viewModel.refresh()
                delay(REFRESH_INTERVAL_MS)
            }
        }
    }
    return badge
}

private const val REFRESH_INTERVAL_MS = 90_000L
