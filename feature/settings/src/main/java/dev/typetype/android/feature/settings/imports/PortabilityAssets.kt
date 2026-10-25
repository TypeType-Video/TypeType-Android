package dev.typetype.android.feature.settings.imports

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

private val FORMAT_NAMES = mapOf(
    "typetype" to "TypeType",
    "pipepipe" to "PipePipe",
    "newpipe" to "NewPipe",
    "invidious" to "Invidious",
    "piped" to "Piped",
    "libretube" to "LibreTube",
    "viewtube" to "ViewTube",
    "materialious" to "Materialious",
    "youtube-local" to "youtube-local",
    "flow" to "Flow",
    "skytube" to "SkyTube",
    "grayjay" to "Grayjay",
    "youtube-takeout" to "YouTube Takeout",
    "opml" to "OPML",
)

internal fun portabilityFormatName(format: String): String =
    FORMAT_NAMES[format] ?: format

@Composable
internal fun PortabilityFormatIcon(format: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val model = remember(context, format) { assetUri(format) }
    if (model.isBlank()) {
        Icon(
            imageVector = Icons.Filled.Archive,
            contentDescription = null,
            modifier = modifier.size(24.dp),
        )
    } else {
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = modifier.size(24.dp),
        )
    }
}

private fun assetUri(format: String): String {
    val fileName = when (format) {
        "typetype" -> "typetype.svg"
        "pipepipe" -> "pipepipe.png"
        "youtube-takeout" -> "youtube-takeout.svg"
        "opml" -> "rss.svg"
        "invidious", "libretube", "newpipe", "piped" -> "$format.svg"
        else -> OFFICIAL_ASSETS[format]
    } ?: return ""
    return "file:///android_asset/portability-formats/$fileName"
}

private val OFFICIAL_ASSETS = mapOf(
    "flow" to "flow.png",
    "grayjay" to "grayjay.png",
    "materialious" to "materialious.png",
    "skytube" to "skytube.png",
    "viewtube" to "viewtube.png",
    "youtube-local" to "youtube-local.ico",
)
