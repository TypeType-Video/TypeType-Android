package dev.typetype.android.feature.settings.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.DropdownRow
import dev.typetype.android.core.ui.components.SettingsSectionHeader
import dev.typetype.android.core.ui.components.SwitchRow

@Composable
fun ContentSettingsRoute(
    onNavigateBack: () -> Unit,
    viewModel: ContentSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ContentSettingsScreen(
        state = state,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun ContentSettingsScreen(
    state: ContentSettingsState,
    onAction: (ContentSettingsAction) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val controlsEnabled = !state.isLoading && !state.isUpdating
    var confirmHideEverything by rememberSaveable { mutableStateOf(false) }
    if (confirmHideEverything) {
        HideEverythingConfirmation(
            onConfirm = {
                confirmHideEverything = false
                onAction(ContentSettingsAction.SetAllHidden(true))
            },
            onDismiss = { confirmHideEverything = false },
        )
    }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            ContentSettingsTopBar(onNavigateBack)
            if (state.isLoading || state.isUpdating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.errorMessage?.let { message ->
                    item {
                        ContentSettingsFailureCard(
                            message = message,
                            requestId = state.errorRequestId,
                            onRetry = { onAction(ContentSettingsAction.Retry) },
                            onDismiss = { onAction(ContentSettingsAction.DismissFailure) },
                        )
                    }
                }
                item { SettingsSectionHeader(stringResource(R.string.settings_content_startup)) }
                item {
                    DropdownRow(
                        title = stringResource(R.string.settings_content_landing_page),
                        subtitle = stringResource(R.string.settings_content_landing_page_subtitle),
                        options = landingPageOptions(),
                        selectedKey = state.defaultLandingPage,
                        onSelect = {
                            onAction(ContentSettingsAction.SetDefaultLandingPage(it))
                        },
                        enabled = controlsEnabled,
                    )
                }
                item { SettingsSectionHeader(stringResource(R.string.settings_content_youtube)) }
                item {
                    SwitchRow(
                        title = stringResource(R.string.settings_content_dearrow),
                        subtitle = stringResource(R.string.settings_content_dearrow_subtitle),
                        checked = state.deArrowEnabled,
                        onCheckedChange = {
                            onAction(ContentSettingsAction.SetDeArrowEnabled(it))
                        },
                        enabled = controlsEnabled,
                    )
                }
                if (state.deArrowEnabled) {
                    deArrowRows(state, controlsEnabled, onAction)
                }
                item { SettingsSectionHeader(stringResource(R.string.settings_content_visibility)) }
                item {
                    SwitchRow(
                        title = stringResource(R.string.settings_content_hide_everything),
                        subtitle = stringResource(R.string.settings_content_hide_everything_subtitle),
                        checked = state.areAllSurfacesHidden(),
                        onCheckedChange = { hidden ->
                            if (hidden) {
                                confirmHideEverything = true
                            } else {
                                onAction(ContentSettingsAction.SetAllHidden(false))
                            }
                        },
                        enabled = controlsEnabled,
                    )
                }
                item {
                    ContentVisibilitySwitch(
                        title = R.string.settings_content_hide_continue,
                        subtitle = R.string.settings_content_hide_continue_subtitle,
                        checked = state.hideContinueWatching,
                        enabled = controlsEnabled,
                        action = ContentSettingsAction::SetHideContinueWatching,
                        onAction = onAction,
                    )
                }
                item {
                    ContentVisibilitySwitch(
                        title = R.string.settings_content_hide_recommendations,
                        subtitle = R.string.settings_content_hide_recommendations_subtitle,
                        checked = state.hideHomeRecommendations,
                        enabled = controlsEnabled,
                        action = ContentSettingsAction::SetHideHomeRecommendations,
                        onAction = onAction,
                    )
                }
                item {
                    ContentVisibilitySwitch(
                        title = R.string.settings_content_hide_related,
                        subtitle = R.string.settings_content_hide_related_subtitle,
                        checked = state.hideRelatedVideos,
                        enabled = controlsEnabled,
                        action = ContentSettingsAction::SetHideRelatedVideos,
                        onAction = onAction,
                    )
                }
                item {
                    ContentVisibilitySwitch(
                        title = R.string.settings_content_hide_comments,
                        subtitle = R.string.settings_content_hide_comments_subtitle,
                        checked = state.hideComments,
                        enabled = controlsEnabled,
                        action = ContentSettingsAction::SetHideComments,
                        onAction = onAction,
                    )
                }
                item {
                    ContentVisibilitySwitch(
                        title = R.string.settings_content_hide_shorts,
                        subtitle = R.string.settings_content_hide_shorts_subtitle,
                        checked = state.hideShorts,
                        enabled = controlsEnabled,
                        action = ContentSettingsAction::SetHideShorts,
                        onAction = onAction,
                    )
                }
                if (state.supportsHideSubscriptionLiveStreams) {
                    item {
                        ContentVisibilitySwitch(
                            title = R.string.settings_content_hide_subscription_lives,
                            subtitle = R.string.settings_content_hide_subscription_lives_subtitle,
                            checked = state.hideSubscriptionLiveStreams,
                            enabled = controlsEnabled,
                            action = ContentSettingsAction::SetHideSubscriptionLiveStreams,
                            onAction = onAction,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun landingPageOptions() = listOf(
    "home" to stringResource(R.string.tab_home),
    "subscriptions" to stringResource(R.string.tab_subscriptions),
    "history" to stringResource(R.string.library_tab_history),
    "playlists" to stringResource(R.string.library_tab_playlists),
    "watch-later" to stringResource(R.string.library_tab_watch_later),
    "favorites" to stringResource(R.string.library_tab_favorites),
)

private fun androidx.compose.foundation.lazy.LazyListScope.deArrowRows(
    state: ContentSettingsState,
    enabled: Boolean,
    onAction: (ContentSettingsAction) -> Unit,
) {
    item {
        DropdownRow(
            title = stringResource(R.string.settings_content_dearrow_titles),
            subtitle = stringResource(R.string.settings_content_dearrow_titles_subtitle),
            options = listOf(
                "dearrow" to stringResource(R.string.settings_content_dearrow_title_community),
                "original" to stringResource(R.string.settings_content_dearrow_title_original),
            ),
            selectedKey = state.deArrowTitleMode,
            onSelect = { onAction(ContentSettingsAction.SetDeArrowTitleMode(it)) },
            enabled = enabled,
        )
    }
    item {
        DropdownRow(
            title = stringResource(R.string.settings_content_dearrow_thumbnails),
            subtitle = stringResource(R.string.settings_content_dearrow_thumbnails_subtitle),
            options = listOf(
                "dearrow_or_random" to stringResource(R.string.settings_content_dearrow_thumbnail_fallback),
                "dearrow" to stringResource(R.string.settings_content_dearrow_thumbnail_community),
                "random" to stringResource(R.string.settings_content_dearrow_thumbnail_neutral),
                "original" to stringResource(R.string.settings_content_dearrow_thumbnail_original),
            ),
            selectedKey = state.deArrowThumbnailMode,
            onSelect = { onAction(ContentSettingsAction.SetDeArrowThumbnailMode(it)) },
            enabled = enabled,
        )
    }
    item {
        DropdownRow(
            title = stringResource(R.string.settings_content_dearrow_confidence),
            subtitle = stringResource(R.string.settings_content_dearrow_confidence_subtitle),
            options = listOf(
                "accepted" to stringResource(R.string.settings_content_dearrow_accepted),
                "locked" to stringResource(R.string.settings_content_dearrow_locked),
            ),
            selectedKey = state.deArrowTrustMode,
            onSelect = { onAction(ContentSettingsAction.SetDeArrowTrustMode(it)) },
            enabled = enabled,
        )
    }
}

@Composable
private fun ContentVisibilitySwitch(
    title: Int,
    subtitle: Int,
    checked: Boolean,
    enabled: Boolean,
    action: (Boolean) -> ContentSettingsAction,
    onAction: (ContentSettingsAction) -> Unit,
) {
    SwitchRow(
        title = stringResource(title),
        subtitle = stringResource(subtitle),
        checked = checked,
        onCheckedChange = { onAction(action(it)) },
        enabled = enabled,
    )
}

@Composable
private fun ContentSettingsTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.settings_back),
            )
        }
        Text(
            text = stringResource(R.string.settings_content_title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
            ),
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
