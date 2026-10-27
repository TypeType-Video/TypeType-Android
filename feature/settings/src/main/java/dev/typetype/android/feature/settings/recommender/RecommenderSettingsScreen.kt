package dev.typetype.android.feature.settings.recommender

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.LocalAppSnackbarHost
import dev.typetype.android.domain.recommendations.ProfileChannel
import dev.typetype.android.feature.settings.SettingsDetailTopBar

@Composable
fun RecommenderSettingsRoute(onNavigateBack: () -> Unit) {
    val viewModel: RecommenderSettingsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    RecommenderSettingsScreen(state = state, onAction = viewModel::onAction, onNavigateBack = onNavigateBack)
}

@Composable
internal fun RecommenderSettingsScreen(
    state: RecommenderSettingsState,
    onAction: (RecommenderSettingsAction) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val inside = state.section != null
    BackHandler(enabled = inside) { onAction(RecommenderSettingsAction.Back) }
    val snackbar = LocalAppSnackbarHost.current
    val chosen = state.chosenAlgorithm?.let { stringResource(R.string.algo_guide_chosen, it) }
    LaunchedEffect(chosen) {
        if (chosen != null) {
            onAction(RecommenderSettingsAction.ChosenShown)
            snackbar?.showSnackbar(chosen)
        }
    }
    val title = state.section?.let { stringResource(it.titleRes) } ?: stringResource(R.string.algo_title)
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsDetailTopBar(
                title = title,
                onNavigateBack = { if (inside) onAction(RecommenderSettingsAction.Back) else onNavigateBack() },
            )
            AnimatedVisibility(visible = state.isSaving) { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            AnimatedVisibility(visible = state.saveFailed) {
                Text(
                    stringResource(R.string.algo_save_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            Box(modifier = Modifier.weight(1f)) { RecommenderBody(state, onAction) }
        }
    }
}

@Composable
private fun RecommenderBody(state: RecommenderSettingsState, onAction: (RecommenderSettingsAction) -> Unit) {
    val retry = { onAction(RecommenderSettingsAction.Retry) }
    when (state.availability) {
        RecommenderAvailability.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        RecommenderAvailability.Guest -> RecommenderNotice(stringResource(R.string.algo_sign_in))
        RecommenderAvailability.NotInstalled -> RecommenderNotice(stringResource(R.string.algo_not_available))
        RecommenderAvailability.NotOffered -> RecommenderNotice(stringResource(R.string.algo_not_offered), stringResource(R.string.algo_retry), retry)
        RecommenderAvailability.Unreachable -> RecommenderNotice(stringResource(R.string.algo_unreachable), stringResource(R.string.algo_retry), retry)
        RecommenderAvailability.Ready -> AnimatedContent(
            targetState = state.section,
            transitionSpec = {
                val forward = targetState != null
                (slideInHorizontally { if (forward) it / 4 else -it / 4 } + fadeIn()) togetherWith
                    (slideOutHorizontally { if (forward) -it / 4 else it / 4 } + fadeOut())
            },
            label = "recommender-section",
        ) { section -> SectionList(state, section, onAction) }
    }
}

@Composable
private fun SectionList(state: RecommenderSettingsState, section: RecommenderSection?, onAction: (RecommenderSettingsAction) -> Unit) {
    val listState = rememberLazyListState()
    var showAllInterests by rememberSaveable(section) { mutableStateOf(false) }
    var openFolds by rememberSaveable(section) { mutableStateOf(emptyList<String>()) }
    var openChannel by rememberSaveable(section) { mutableStateOf<String?>(null) }
    val highlightIndex = if (section == RecommenderSection.Profile) {
        state.highlight?.let { id -> shownInterests(state, showAllInterests).indexOfFirst { it.id == id } }?.takeIf { it >= 0 }
    } else {
        null
    }
    LaunchedEffect(highlightIndex) {
        highlightIndex?.let { listState.animateScrollToItem(it + PROFILE_HEADER_ITEMS) }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (section != null) item(key = "description") { RecommenderNote(stringResource(section.descriptionRes)) }
        when (section) {
            null -> recommenderHub(state, onAction)
            RecommenderSection.Overview -> overviewSection(state, onAction)
            RecommenderSection.Algorithm -> algorithmSection(state, onAction)
            RecommenderSection.Guide -> guideSection(state, onAction)
            RecommenderSection.Data -> dataSection(state, onAction)
            RecommenderSection.Profile -> profileSection(
                state = state,
                showAll = showAllInterests,
                onShowAll = { showAllInterests = true },
                onOpenChannel = { channel: ProfileChannel -> openChannel = channel.id },
                onAction = onAction,
            )
            RecommenderSection.Details -> detailsSection(
                state = state,
                open = openFolds.toSet(),
                onToggle = { id -> openFolds = if (id in openFolds) openFolds - id else openFolds + id },
                onAction = onAction,
            )
            RecommenderSection.Feedback -> feedbackSection(state, onAction)
        }
    }
    val channel = state.profile.value?.channels?.firstOrNull { it.id == openChannel }
    if (channel != null) {
        ChannelSheet(
            channel = channel,
            ignored = channel.id in state.preferences.ignoredSignals,
            onToggle = { onAction(RecommenderSettingsAction.ToggleIgnored(channel.id)) },
            onDismiss = { openChannel = null },
        )
    }
}

private const val PROFILE_HEADER_ITEMS = 2
