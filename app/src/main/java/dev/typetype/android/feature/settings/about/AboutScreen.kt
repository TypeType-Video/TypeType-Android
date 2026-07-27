package dev.typetype.android.feature.settings.about

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.BuildConfig
import dev.typetype.android.R

private data class AboutLink(val labelRes: Int, val url: String)

private val LINKS = listOf(
    AboutLink(R.string.about_link_typetype, "https://github.com/Priveetee/TypeType"),
    AboutLink(R.string.about_link_typetype_android, "https://github.com/Priveetee/TypeType-Android"),
)

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    viewModel: AboutViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
            TopBar(onNavigateBack = onNavigateBack)
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { AppHeader() }
                item { Spacer(Modifier.size(8.dp)) }
                item { SectionHeader(stringResource(R.string.about_section_app)) }
                item {
                    InfoCard {
                        InfoRow(label = stringResource(R.string.about_app_name), value = "TypeType")
                        InfoRow(label = stringResource(R.string.about_version), value = BuildConfig.VERSION_NAME)
                        InfoRow(label = "License", value = "GPL v3")
                    }
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
                item { Spacer(Modifier.size(8.dp)) }
                item { SectionHeader(stringResource(R.string.about_section_links)) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        LINKS.forEach { link ->
                            LinkRow(
                                label = stringResource(link.labelRes),
                                url = link.url,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, link.url.toUri())
                                    runCatching { context.startActivity(intent) }
                                },
                            )
                        }
                    }
                }
                item {
                    Text(
                        text = stringResource(R.string.about_credits),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.settings_back),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = stringResource(R.string.settings_about_title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 4.dp),
        )
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
                .clip(RoundedCornerShape(14.dp))
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
                text = stringResource(R.string.about_version),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface),
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
}

@Composable
private fun LinkRow(label: String, url: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
