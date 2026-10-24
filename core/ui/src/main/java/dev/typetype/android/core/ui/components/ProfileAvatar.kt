package dev.typetype.android.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import dev.typetype.android.core.openmoji.openMojiUrl
import dev.typetype.android.core.openmoji.pickOpenMojiCode
import dev.typetype.android.core.ui.share.LocalServerBaseUrl
import dev.typetype.android.core.ui.share.buildImageUrl
import java.net.URI

@Composable
fun ProfileAvatar(
    imageUrl: String?,
    fallbackLetter: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    var loaded by remember(imageUrl) { mutableStateOf(false) }
    val context = LocalPlatformContext.current
    val serverBaseUrl = LocalServerBaseUrl.current
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (contentDescription == null) Modifier
                else Modifier.semantics { this.contentDescription = contentDescription },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (!loaded) {
            Text(
                text = fallbackLetter?.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(buildImageUrl(serverBaseUrl, imageUrl))
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = { loaded = true },
                onError = { loaded = false },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

fun resolveProfileAvatarUrl(
    serverBaseUrl: String?,
    avatarUrl: String?,
    avatarType: String?,
    avatarCode: String?,
    fallbackSeed: String,
): String? {
    if (avatarType.equals("emoji", ignoreCase = true) && !avatarCode.isNullOrBlank()) {
        return openMojiUrl(serverBaseUrl, avatarCode)
    }
    val direct = avatarUrl?.trim().orEmpty()
    if (direct.isNotEmpty()) return resolveAvatarReference(serverBaseUrl, direct)
    return openMojiUrl(serverBaseUrl, pickOpenMojiCode(fallbackSeed))
}

private fun resolveAvatarReference(serverBaseUrl: String?, reference: String): String? {
    val parsed = runCatching { URI(reference) }.getOrNull() ?: return null
    if (parsed.isAbsolute) {
        return reference.takeIf {
            parsed.scheme.equals("https", ignoreCase = true) ||
                parsed.scheme.equals("http", ignoreCase = true)
        }
    }
    val base = serverBaseUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (reference.startsWith('/')) {
        return "${base.trimEnd('/')}$reference"
    }
    return runCatching { URI("${base.trimEnd('/')}/").resolve(parsed).toString() }.getOrNull()
}
