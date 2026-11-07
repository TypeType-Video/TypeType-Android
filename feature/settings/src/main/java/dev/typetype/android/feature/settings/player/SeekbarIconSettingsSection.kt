package dev.typetype.android.feature.settings.player

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.DropdownRow
import dev.typetype.android.core.ui.components.SettingsSectionHeader
import dev.typetype.android.domain.usersettings.SEEKBAR_ICON_CUSTOM
import dev.typetype.android.domain.usersettings.SEEKBAR_ICON_MEDIA_TYPES
import dev.typetype.android.domain.usersettings.SEEKBAR_ICON_NONE
import dev.typetype.android.domain.usersettings.SEEKBAR_ICON_TYPETYPE

@Composable
internal fun SeekbarIconSettingsSection(viewModel: SeekbarIconSettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::upload)
    }
    val pickImage = { picker.launch(SEEKBAR_ICON_MEDIA_TYPES.toTypedArray()) }
    val hasCustom = state.customUrl != null
    Column {
        SettingsSectionHeader(stringResource(R.string.settings_seekbar_icon_title))
        DropdownRow(
            title = stringResource(R.string.settings_seekbar_icon_title),
            subtitle = stringResource(R.string.settings_seekbar_icon_subtitle),
            options = listOf(
                SEEKBAR_ICON_TYPETYPE to stringResource(R.string.settings_seekbar_icon_typetype),
                SEEKBAR_ICON_CUSTOM to stringResource(R.string.settings_seekbar_icon_custom),
                SEEKBAR_ICON_NONE to stringResource(R.string.settings_seekbar_icon_none),
            ),
            selectedKey = if (state.icon == SEEKBAR_ICON_CUSTOM && !hasCustom) SEEKBAR_ICON_TYPETYPE else state.icon,
            onSelect = { key -> if (key == SEEKBAR_ICON_CUSTOM && !hasCustom) pickImage() else viewModel.select(key) },
            enabled = !state.busy,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = pickImage, enabled = !state.busy) {
                Text(
                    stringResource(
                        if (hasCustom) R.string.settings_seekbar_icon_replace else R.string.settings_seekbar_icon_upload,
                    ),
                )
            }
            if (hasCustom) {
                TextButton(onClick = viewModel::remove, enabled = !state.busy) {
                    Text(stringResource(R.string.settings_seekbar_icon_remove))
                }
            }
        }
        Text(
            text = stringResource(R.string.settings_seekbar_icon_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        state.error?.let { error ->
            Text(
                text = stringResource(error.messageRes()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        SeekbarIconPreview(icon = state.icon, customUrl = state.customUrl)
    }
}

@Composable
private fun SeekbarIconPreview(icon: String, customUrl: String?) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackShape = RoundedCornerShape(2.dp)
        Box(Modifier.fillMaxWidth().height(4.dp).background(MaterialTheme.colorScheme.outlineVariant, trackShape))
        Box(Modifier.fillMaxWidth(0.4f).height(4.dp).background(MaterialTheme.colorScheme.primary, trackShape))
        val iconModifier = Modifier.offset(x = maxWidth * 0.4f - 14.dp).size(28.dp)
        when {
            icon == SEEKBAR_ICON_NONE -> Box(
                iconModifier.padding(7.dp).background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(50)),
            )
            icon == SEEKBAR_ICON_CUSTOM && customUrl != null -> AsyncImage(
                model = customUrl,
                contentDescription = null,
                modifier = iconModifier,
            )
            else -> Image(
                painter = painterResource(R.drawable.seekbar_typetype),
                contentDescription = null,
                modifier = iconModifier,
            )
        }
    }
}

private fun SeekbarIconError.messageRes(): Int = when (this) {
    SeekbarIconError.Unsupported -> R.string.settings_seekbar_icon_error_format
    SeekbarIconError.TooLarge -> R.string.settings_seekbar_icon_error_size
    SeekbarIconError.Failed -> R.string.settings_seekbar_icon_error_upload
}
