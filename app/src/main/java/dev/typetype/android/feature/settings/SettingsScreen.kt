package dev.typetype.android.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.typetype.android.R

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenProfile: () -> Unit = {},
    onOpenImport: () -> Unit = {},
    importsAvailable: Boolean = false,
    onOpenYoutubeSession: () -> Unit = {},
    youtubeSessionAvailable: Boolean = false,
    onOpenAccounts: () -> Unit = {},
    onOpenAppearance: () -> Unit,
    onOpenContent: () -> Unit = {},
    onOpenPlayer: () -> Unit = {},
    onOpenStorage: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenDiagnostics: () -> Unit = {},
    onOpenBlocked: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onSignOut: () -> Unit,
) {
    val entries = buildList {
        add(SettingsEntry(R.string.accounts_title, R.string.accounts_subtitle, Icons.Filled.ManageAccounts, onOpenAccounts))
        add(SettingsEntry(R.string.settings_profile_title, R.string.settings_profile_subtitle, Icons.Filled.Person, onOpenProfile))
        if (importsAvailable) {
            add(SettingsEntry(R.string.settings_import_title, R.string.settings_import_subtitle, Icons.Filled.UploadFile, onOpenImport))
        }
        if (youtubeSessionAvailable) {
            add(
                SettingsEntry(
                    R.string.youtube_session_settings_title,
                    R.string.youtube_session_settings_subtitle,
                    Icons.Filled.Link,
                    onOpenYoutubeSession,
                ),
            )
        }
        add(SettingsEntry(R.string.settings_appearance_title, R.string.settings_appearance_subtitle, Icons.Filled.Palette, onOpenAppearance))
        add(SettingsEntry(R.string.settings_content_title, R.string.settings_content_subtitle, Icons.Filled.Tune, onOpenContent))
        add(SettingsEntry(R.string.settings_player_title, R.string.settings_player_subtitle, Icons.Filled.PlayCircle, onOpenPlayer))
        add(SettingsEntry(R.string.settings_storage_title, R.string.settings_storage_subtitle, Icons.Filled.Storage, onOpenStorage))
        add(SettingsEntry(R.string.settings_privacy_title, R.string.settings_privacy_subtitle, Icons.Filled.Lock, onOpenPrivacy))
        add(SettingsEntry(R.string.diagnostics_title, R.string.diagnostics_subtitle, Icons.Filled.BugReport, onOpenDiagnostics))
        add(SettingsEntry(R.string.settings_blocked_title, R.string.settings_blocked_subtitle, Icons.Filled.Block, onOpenBlocked))
        add(SettingsEntry(R.string.settings_about_title, R.string.settings_about_subtitle, Icons.Filled.Info, onOpenAbout))
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            SettingsTopBar(onNavigateBack = onNavigateBack)
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(entries) { entry -> SettingsCategoryCard(entry = entry) }
                item {
                    Spacer(Modifier.size(16.dp))
                    SignOutRow(onClick = onSignOut)
                }
            }
        }
    }
}

@Composable
private fun SettingsTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
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
            text = stringResource(R.string.settings_title),
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
private fun SettingsCategoryCard(entry: SettingsEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = entry.onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(entry.titleRes),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(entry.subtitleRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SignOutRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = stringResource(R.string.settings_sign_out),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.error,
        )
    }
}

private data class SettingsEntry(
    val titleRes: Int,
    val subtitleRes: Int,
    val icon: ImageVector,
    val onClick: () -> Unit,
)
