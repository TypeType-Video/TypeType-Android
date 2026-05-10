package dev.typetype.android.feature.library.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.library.VideoMeta
import dev.typetype.android.domain.library.VideoMetaRepository
import dev.typetype.android.domain.stream.StreamRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private const val PARALLEL_PREFETCH = 3

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VideoMetaLookupViewModel @Inject constructor(
    private val metaRepo: VideoMetaRepository,
    private val streamRepo: StreamRepository,
) : ViewModel() {

    private val urlsFlow = MutableStateFlow<List<String>>(emptyList())

    val metas: StateFlow<Map<String, VideoMeta>> = urlsFlow
        .flatMapLatest { urls -> metaRepo.observeForUrls(urls) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val semaphore = Semaphore(PARALLEL_PREFETCH)
    private val inFlight = mutableSetOf<String>()
    private val resolved = mutableSetOf<String>()
    private val mutex = Any()
    private var prefetchJob: Job? = null

    fun setUrls(urls: List<String>) {
        urlsFlow.value = urls
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch {
            val cached = metaRepo.observeForUrls(urls).first()
            val pending = synchronized(mutex) {
                urls.filter { url ->
                    val cachedMeta = cached[url]
                    val needsFetch = cachedMeta == null ||
                        cachedMeta.channelAvatarUrl.isBlank() ||
                        cachedMeta.channelName.isBlank()
                    needsFetch && url !in inFlight && url !in resolved
                }.also { fresh -> inFlight.addAll(fresh) }
            }
            pending.forEach { url ->
                launch {
                    semaphore.withPermit { fetchMeta(url) }
                }
            }
        }
    }

    private suspend fun fetchMeta(url: String) {
        val result = streamRepo.loadStream(url)
        synchronized(mutex) {
            inFlight.remove(url)
            resolved.add(url)
        }
        result.getOrNull()?.let { stream ->
            metaRepo.put(
                VideoMeta(
                    videoUrl = url,
                    channelName = stream.uploaderName,
                    channelUrl = stream.uploaderUrl,
                    channelAvatarUrl = stream.uploaderAvatarUrl,
                    viewCount = stream.viewCount,
                ),
            )
        }
    }
}

@Composable
fun rememberVideoMetas(urls: List<String>): Map<String, VideoMeta> {
    val vm: VideoMetaLookupViewModel = hiltViewModel()
    LaunchedEffect(urls) { vm.setUrls(urls) }
    val metas by vm.metas.collectAsStateWithLifecycle()
    return metas
}
