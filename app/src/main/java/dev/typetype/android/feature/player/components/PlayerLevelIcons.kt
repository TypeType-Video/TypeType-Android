package dev.typetype.android.feature.player.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.ui.graphics.vector.ImageVector

internal fun brightnessLevelIcon(fraction: Float): ImageVector = when {
    fraction < 0.25f -> Icons.Filled.BrightnessLow
    fraction < 0.75f -> Icons.Filled.BrightnessMedium
    else -> Icons.Filled.BrightnessHigh
}

internal fun volumeLevelIcon(fraction: Float): ImageVector = when {
    fraction <= 0f -> Icons.AutoMirrored.Filled.VolumeOff
    fraction < 0.25f -> Icons.AutoMirrored.Filled.VolumeMute
    fraction < 0.75f -> Icons.AutoMirrored.Filled.VolumeDown
    else -> Icons.AutoMirrored.Filled.VolumeUp
}
