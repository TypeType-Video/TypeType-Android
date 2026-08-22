package dev.typetype.android.feature.player.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.feature.player.state.ResizeMode

@Composable
internal fun PlayerTopBar(
    title: String,
    autoplayEnabled: Boolean,
    onAutoplayChange: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenChapters: () -> Unit,
    onOpenOptions: () -> Unit,
    onEnterPip: () -> Unit,
    onCycleResizeMode: () -> Unit,
    resizeMode: ResizeMode,
    isFullscreen: Boolean,
    isPipAvailable: Boolean,
    chaptersAvailable: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        BackButton(onNavigateBack)
        if (isFullscreen) {
            Text(
                text = title,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
        } else {
            Box(modifier = Modifier.weight(1f))
        }
        TopActions(
            autoplayEnabled,
            onAutoplayChange,
            onOpenChapters,
            onOpenOptions,
            onEnterPip,
            onCycleResizeMode,
            resizeMode,
            isFullscreen,
            isPipAvailable,
            chaptersAvailable,
        )
    }
}

@Composable
private fun BackButton(onNavigateBack: () -> Unit) {
    OverlayIconButton(onClick = onNavigateBack, modifier = Modifier.padding(8.dp)) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.player_back),
            tint = Color.White,
        )
    }
}

@Composable
private fun TopActions(
    autoplayEnabled: Boolean,
    onAutoplayChange: (Boolean) -> Unit,
    onOpenChapters: () -> Unit,
    onOpenOptions: () -> Unit,
    onEnterPip: () -> Unit,
    onCycleResizeMode: () -> Unit,
    resizeMode: ResizeMode,
    isFullscreen: Boolean,
    isPipAvailable: Boolean,
    chaptersAvailable: Boolean,
) {
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isFullscreen) {
            AutoplaySwitch(autoplayEnabled, onAutoplayChange)
            OverlayIconButton(onCycleResizeMode) {
                Icon(
                    imageVector = resizeMode.icon(),
                    contentDescription = stringResource(R.string.player_resize_mode),
                    tint = Color.White,
                )
            }
        }
        if (isPipAvailable) {
            OverlayIconButton(onEnterPip) {
                Icon(
                    painter = painterResource(R.drawable.ic_pip),
                    contentDescription = stringResource(R.string.player_pip),
                    tint = Color.White,
                )
            }
        }
        if (chaptersAvailable) {
            OverlayIconButton(onOpenChapters) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = stringResource(R.string.player_chapters),
                    tint = Color.White,
                )
            }
        }
        OverlayIconButton(onOpenOptions) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.player_playback_options),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun AutoplaySwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val label = stringResource(R.string.player_autoplay_short)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, color = Color.White, style = MaterialTheme.typography.labelMedium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}

@Composable
private fun OverlayIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.padding(2.dp).size(40.dp),
        content = content,
    )
}

private fun ResizeMode.icon(): ImageVector = when (this) {
    ResizeMode.Fit -> Icons.Filled.FitScreen
    ResizeMode.Crop -> Icons.Filled.Crop
    ResizeMode.Stretch -> Icons.Filled.AspectRatio
}
