package video.typetype.tv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import video.typetype.sdk.core.Channel
import video.typetype.sdk.core.Video
import video.typetype.tv.data.TvAppState
import video.typetype.tv.data.TvSubscriptionGroupActions

@OptIn(ExperimentalFoundationApi::class)
@Composable
public fun HomeScreen(
    state: TvAppState,
    isActive: Boolean,
    onPlayVideo: (Video) -> Unit,
    onOpenVideo: (Video) -> Unit,
    onOpenChannel: (Channel) -> Unit,
    heroFocusRequester: FocusRequester,
    subscriptionGroupActions: TvSubscriptionGroupActions?,
) {
    val featured = remember(state.home, state.trending) {
        state.home.plus(state.trending)
            .distinctBy { it.serviceId to it.id.value }
            .take(FEATURED_COUNT)
    }
    var highlighted by remember(featured.firstOrNull()?.id) { mutableStateOf(featured.firstOrNull()) }
    val focusMemory = rememberSaveable(
        saver = androidx.compose.runtime.saveable.Saver(
            save = { it.key },
            restore = { HomeFocusMemory(it) },
        ),
    ) { HomeFocusMemory() }
    var previewJob by remember { mutableStateOf<Job?>(null) }
    val previewScope = rememberCoroutineScope()
    val schedulePreview: (Video) -> Unit = { video ->
        previewJob?.cancel()
        previewJob = previewScope.launch {
            delay(PREVIEW_DELAY_MILLISECONDS)
            highlighted = video
        }
    }
    val listState = rememberLazyListState()
    if (state.isLoading && featured.isEmpty()) {
        LoadingScreen()
        return
    }
    if (!state.isLoading && featured.isEmpty()) {
        EmptyScreen(
            title = "Nothing to show yet",
            message = state.errorMessage ?: "Refresh the home screen when your instance is available.",
        )
        return
    }
    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalBringIntoViewSpec provides TvBringIntoViewSpec) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(bottom = 72.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (featured.isNotEmpty()) item {
                    FeaturedShelf(
                        selected = highlighted ?: featured.first(),
                        label = if (state.settings.hideHomeRecommendations) "Featured" else "Featured for you",
                        playFocusRequester = heroFocusRequester,
                        onPlay = onPlayVideo,
                        onDetails = onOpenVideo,
                        onHeroFocused = { previewScope.launch { listState.animateScrollToItem(0) } },
                    )
                }
                if (featured.isNotEmpty() && !state.settings.hideHomeRecommendations) item {
                    HomeVideoRow(
                        "Recommended for you",
                        featured,
                        isActive,
                        focusMemory.key,
                        onOpenVideo,
                        schedulePreview,
                    ) { focusMemory.key = it }
                }
                val continueWatching = if (state.settings.hideContinueWatching) {
                    emptyList()
                } else {
                    state.history.filter { it.progressMilliseconds > 0L }
                }
                if (continueWatching.isNotEmpty()) item {
                    VideoRow(
                        title = "Continue watching",
                        videos = continueWatching.map { it.video },
                        onOpenVideo = onOpenVideo,
                        progressByVideoId = continueWatching.associate { it.video.id.value to it.progressMilliseconds },
                        restoreFocusKey = focusMemory.key,
                        focusActive = isActive,
                        onFocused = { focusMemory.key = it },
                        onPreviewVideo = schedulePreview,
                        cinematic = false,
                    )
                }
                state.home.drop(FEATURED_COUNT).takeIf { it.isNotEmpty() }?.let { more ->
                    item { HomeVideoRow("More for you", more, isActive, focusMemory.key, onOpenVideo, schedulePreview) { focusMemory.key = it } }
                }
                if (state.subscriptionGroups.isNotEmpty()) item {
                    SubscriptionGroupSelector(
                        groups = state.subscriptionGroups,
                        selectedGroupId = state.selectedSubscriptionGroupId,
                        enabled = !state.isActionInProgress,
                        onSelect = { subscriptionGroupActions?.select?.invoke(it) },
                    )
                }
                if (state.subscriptionFeed.isNotEmpty()) item {
                    val selectedGroup = state.subscriptionGroups.firstOrNull {
                        it.id == state.selectedSubscriptionGroupId
                    }
                    HomeVideoRow(
                        selectedGroup?.let { "From ${it.name}" } ?: "From your subscriptions",
                        state.subscriptionFeed, isActive, focusMemory.key,
                        onOpenVideo, schedulePreview,
                    ) { focusMemory.key = it }
                }
                if (state.subscriptions.isNotEmpty()) item { SubscriptionRow(state.subscriptions, onOpenChannel) }
                if (state.trending.isNotEmpty()) item {
                    HomeVideoRow("Trending now", state.trending, isActive, focusMemory.key, onOpenVideo, schedulePreview) {
                        focusMemory.key = it
                    }
                }
                if (state.shorts.isNotEmpty()) item {
                    HomeVideoRow("Shorts", state.shorts, isActive, focusMemory.key, onOpenVideo, schedulePreview) {
                        focusMemory.key = it
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeVideoRow(
    title: String,
    videos: List<Video>,
    active: Boolean,
    focusKey: String?,
    onOpen: (Video) -> Unit,
    onPreview: (Video) -> Unit,
    onFocusKey: (String) -> Unit,
) {
    VideoRow(
        title = title,
        videos = videos,
        onOpenVideo = onOpen,
        restoreFocusKey = focusKey,
        focusActive = active,
        onFocused = onFocusKey,
        onPreviewVideo = onPreview,
        cinematic = false,
        revealFocusedDetails = true,
    )
}

private const val FEATURED_COUNT = 6
private const val PREVIEW_DELAY_MILLISECONDS = 1_200L

private class HomeFocusMemory(var key: String? = null)
