package dev.typetype.android.feature.settings.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.BuildConfig
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.SectionHeader
import dev.typetype.android.core.ui.components.KomiStoreAttribution
import dev.typetype.android.feature.settings.SettingsDetailTopBar

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    viewModel: AboutViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsDetailTopBar(
                title = stringResource(R.string.settings_about_title),
                onNavigateBack = onNavigateBack,
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { AppHeader() }
                item { Spacer(Modifier.size(8.dp)) }
                item { SectionHeader(stringResource(R.string.about_section_app)) }
                item {
                    InfoCard {
                        InfoRow(
                            label = stringResource(R.string.about_app_name),
                            value = stringResource(R.string.app_name),
                        )
                        InfoRow(label = stringResource(R.string.about_version), value = BuildConfig.VERSION_NAME)
                        InfoRow(
                            label = stringResource(R.string.about_license),
                            value = stringResource(R.string.about_license_value),
                        )
                    }
                }
                item { Spacer(Modifier.size(8.dp)) }
                item {
                    AboutVersionsSection(
                        versions = state.componentVersions,
                        isLoading = state.isLoadingVersions,
                        onRefresh = viewModel::refreshVersions,
                    )
                }
                if (state.serverBaseUrl.isNotBlank()) {
                    item { Spacer(Modifier.size(8.dp)) }
                    item { SectionHeader(stringResource(R.string.about_section_server)) }
                    item {
                        InfoCard {
                            InfoRow(
                                label = stringResource(R.string.about_server_url),
                                value = state.serverBaseUrl
                                    .removeSuffix("/")
                                    .removeSuffix("/api")
                                    .removeSuffix("/"),
                            )
                            if (state.publicUsername.isNotBlank()) {
                                InfoRow(
                                    label = stringResource(R.string.about_server_signed_in_as),
                                    value = state.publicUsername,
                                )
                            }
                            if (state.role.isNotBlank()) {
                                InfoRow(
                                    label = stringResource(R.string.settings_profile_role),
                                    value = state.role.replaceFirstChar { it.titlecase() },
                                )
                            }
                        }
                    }
                }
                item { KomiStoreAttribution() }
            }
        }
    }
}

@Composable
private fun AppHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_launcher_monochrome),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = stringResource(R.string.about_app_name),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = BuildConfig.VERSION_NAME,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
