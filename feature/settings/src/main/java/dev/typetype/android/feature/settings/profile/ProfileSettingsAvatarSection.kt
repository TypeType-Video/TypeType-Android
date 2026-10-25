package dev.typetype.android.feature.settings.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import dev.typetype.android.R
import dev.typetype.android.core.openmoji.OPENMOJI_CATALOG
import dev.typetype.android.core.openmoji.OpenMojiEntry
import dev.typetype.android.core.openmoji.openMojiUrl
import dev.typetype.android.core.ui.components.ProfileAvatar
import dev.typetype.android.core.ui.components.resolveProfileAvatarUrl
import dev.typetype.android.domain.profile.Profile

@Composable
fun AvatarHeader(profile: Profile?, baseUrl: String?) {
    val resolvedUrl = profile?.let {
        resolveProfileAvatarUrl(
            serverBaseUrl = baseUrl,
            avatarUrl = it.avatarUrl,
            avatarType = it.avatarType,
            avatarCode = it.avatarCode,
            fallbackSeed = "${it.id}:${it.publicUsername}",
        )
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProfileAvatar(
            imageUrl = resolvedUrl,
            fallbackLetter = profile?.publicUsername,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
fun EmojiPicker(
    baseUrl: String?,
    selectedCode: String,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
) {
    var search by remember { mutableStateOf("") }
    val filtered by remember(search) {
        derivedStateOf {
            val term = search.trim().lowercase()
            if (term.isEmpty()) OPENMOJI_CATALOG
            else OPENMOJI_CATALOG.filter {
                it.label.contains(term) || it.code.lowercase().contains(term)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.settings_profile_avatar_emojis_from),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            EmojiSearchField(
                query = search,
                onQueryChange = { search = it },
                modifier = Modifier.width(160.dp),
            )
        }

        if (filtered.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_profile_avatar_no_match),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp),
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(filtered, key = { it.code }) { entry ->
                    EmojiCell(
                        entry = entry,
                        baseUrl = baseUrl,
                        selected = entry.code == selectedCode,
                        onClick = { onSelect(entry.code) },
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onClear) {
                Text(stringResource(R.string.settings_profile_avatar_clear))
            }
        }
    }
}

@Composable
private fun EmojiSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_profile_avatar_search),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun EmojiCell(
    entry: OpenMojiEntry,
    baseUrl: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .clickable(onClick = onClick)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        val url = openMojiUrl(baseUrl, entry.code)
        if (url != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(url).build(),
                contentDescription = entry.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
