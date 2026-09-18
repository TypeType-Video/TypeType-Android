package video.typetype.tv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import video.typetype.sdk.core.ServiceId
import video.typetype.sdk.core.UserProfile
import video.typetype.sdk.core.UserSettings
import video.typetype.tv.data.TvProfileActions
import video.typetype.tv.ui.theme.TvAppearance

@Composable
public fun AppearanceScreen(
    profile: UserProfile?,
    settings: UserSettings,
    appearance: TvAppearance,
    services: List<ServiceId>,
    selectedService: ServiceId,
    onServiceChange: (ServiceId) -> Unit,
    onSettingsChange: (UserSettings) -> Unit,
    onChange: (TvAppearance) -> Unit,
    profileActions: TvProfileActions?,
    isActionInProgress: Boolean,
    onLogout: () -> Unit,
    initialFocus: FocusRequester,
    topNavigationFocus: FocusRequester,
) {
    val profileFocus = androidx.compose.runtime.remember { FocusRequester() }
    val contentFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val backgroundColor = MaterialTheme.colorScheme.background
    val showListFade by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }
    var section by rememberSaveable { mutableStateOf(SettingsSection.PLAYBACK) }
    var showProfileEditor by rememberSaveable { mutableStateOf(false) }
    var confirmingLogout by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxSize().padding(start = 58.dp, top = 82.dp, end = 58.dp, bottom = 38.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Text("Settings", style = MaterialTheme.typography.displaySmall)
            Text(
                "Shape TypeType around the way you watch.",
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingsSectionSelector(
                selected = section,
                initialFocus = initialFocus,
                topNavigationFocus = topNavigationFocus,
                contentFocus = contentFocus,
                onSelect = { selected ->
                    section = selected
                    scope.launch { listState.scrollToItem(0) }
                },
            )
            Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 18.dp)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    when (section) {
                        SettingsSection.PLAYBACK -> playbackPreferenceItems(
                            settings, services, selectedService, contentFocus, profileFocus, onServiceChange, onSettingsChange,
                        )
                        SettingsSection.CONTENT -> contentPreferenceItems(settings, contentFocus, profileFocus, onSettingsChange)
                        SettingsSection.SPONSOR_BLOCK -> sponsorBlockPreferenceItems(
                            settings, contentFocus, profileFocus, onSettingsChange,
                        )
                        SettingsSection.APPEARANCE -> appearancePreferenceItems(
                            appearance, contentFocus, profileFocus, onChange,
                        )
                    }
                }
                if (showListFade) {
                    Box(
                        Modifier.fillMaxWidth().height(44.dp).align(Alignment.TopCenter).drawWithContent {
                            drawRect(
                                Brush.verticalGradient(
                                    0f to backgroundColor,
                                    .65f to backgroundColor,
                                    1f to backgroundColor.copy(alpha = 0f),
                                ),
                            )
                        },
                    )
                }
            }
        }
        AppearanceSummaryCard(
            profile = profile,
            appearance = appearance,
            canEditProfile = profileActions != null,
            firstActionFocus = profileFocus,
            returnFocus = initialFocus,
            onEditProfile = { showProfileEditor = true },
            onLogout = { confirmingLogout = true },
        )
    }
    if (showProfileEditor && profileActions != null) {
        ProfileEditorDialog(
            profile = profile,
            isActionInProgress = isActionInProgress,
            actions = profileActions,
            onDismiss = { showProfileEditor = false },
        )
    }
    if (confirmingLogout) {
        TvConfirmDialog(
            title = "Sign out?",
            message = "You will need to sign in again to access this TypeType account on the TV.",
            confirmLabel = "Sign out",
            onDismiss = { confirmingLogout = false },
            onConfirm = {
                confirmingLogout = false
                onLogout()
            },
        )
    }
}

@Composable
private fun SettingsSectionSelector(
    selected: SettingsSection,
    initialFocus: FocusRequester,
    topNavigationFocus: FocusRequester,
    contentFocus: FocusRequester,
    onSelect: (SettingsSection) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SettingsSection.entries.forEachIndexed { index, section ->
            val active = section == selected
            Surface(
                modifier = Modifier
                    .then(if (active) Modifier.focusRequester(initialFocus) else Modifier)
                    .focusProperties {
                        up = topNavigationFocus
                        down = contentFocus
                        if (index == SettingsSection.entries.lastIndex) right = FocusRequester.Cancel
                    },
                onClick = { onSelect(section) },
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.primary,
                    focusedContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Box(Modifier.padding(horizontal = 22.dp, vertical = 12.dp)) {
                    Text(section.label, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

private enum class SettingsSection(val label: String) {
    PLAYBACK("Playback"),
    CONTENT("Content"),
    SPONSOR_BLOCK("SponsorBlock"),
    APPEARANCE("Appearance"),
}
