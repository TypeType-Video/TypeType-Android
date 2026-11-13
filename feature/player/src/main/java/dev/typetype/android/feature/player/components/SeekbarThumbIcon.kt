package dev.typetype.android.feature.player.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import dev.typetype.android.R
import dev.typetype.android.domain.usersettings.SEEKBAR_ICON_CUSTOM
import dev.typetype.android.domain.usersettings.SEEKBAR_ICON_NONE
import dev.typetype.android.domain.usersettings.UserSettings

@Immutable
sealed interface SeekbarThumbIcon {
    data object Plain : SeekbarThumbIcon
    data object TypeType : SeekbarThumbIcon
    data class Remote(val url: String) : SeekbarThumbIcon
}

val LocalSeekbarThumbIcon = staticCompositionLocalOf<SeekbarThumbIcon> { SeekbarThumbIcon.TypeType }

fun UserSettings.seekbarThumbIcon(): SeekbarThumbIcon = when (seekbarIcon) {
    SEEKBAR_ICON_NONE -> SeekbarThumbIcon.Plain
    SEEKBAR_ICON_CUSTOM -> seekbarIconUrl?.let(SeekbarThumbIcon::Remote) ?: SeekbarThumbIcon.TypeType
    else -> SeekbarThumbIcon.TypeType
}

@Composable
internal fun SeekbarThumbImage(icon: SeekbarThumbIcon, modifier: Modifier = Modifier) {
    when (icon) {
        SeekbarThumbIcon.Plain -> Unit
        SeekbarThumbIcon.TypeType -> Image(
            painter = painterResource(R.drawable.seekbar_typetype),
            contentDescription = null,
            modifier = modifier,
        )
        is SeekbarThumbIcon.Remote -> AsyncImage(
            model = icon.url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            error = painterResource(R.drawable.seekbar_typetype),
            modifier = modifier,
        )
    }
}
