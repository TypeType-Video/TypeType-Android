package dev.typetype.android.feature.player

import dev.typetype.android.domain.preferences.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class PlaybackBrightnessPreferenceController(
    private val preferencesRepository: PreferencesRepository,
    private val scope: CoroutineScope,
) {
    private var saveJob: Job? = null

    fun update(percent: Int, onChanged: (Int) -> Unit) {
        val value = percent.coerceIn(0, 100)
        onChanged(value)
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(SAVE_DEBOUNCE_MS)
            preferencesRepository.setPlayerPlaybackBrightnessPercent(value)
        }
    }
}

private const val SAVE_DEBOUNCE_MS = 180L
