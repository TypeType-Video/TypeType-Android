package dev.typetype.android.feature.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.progress.WatchProgressSource
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class WatchProgressViewModel @Inject constructor(private val source: WatchProgressSource) : ViewModel() {
    val positions: StateFlow<Map<String, Long>> = source.positions

    fun load(videoUrls: List<String>) {
        if (videoUrls.isEmpty()) return
        viewModelScope.launch { source.load(videoUrls) }
    }
}
