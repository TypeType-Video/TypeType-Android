package dev.typetype.android.feature.settings.updates

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.domain.appupdate.AppUpdateRelease

@Composable
internal fun ReleaseNotesCard(release: AppUpdateRelease) {
    val blocks = remember(release.releaseNotes, release.versionName) {
        parseReleaseNotes(release.releaseNotes).dropRepeatedTitle(release.versionName)
    }
    var expanded by rememberSaveable(release.tagName) { mutableStateOf(false) }
    val collapsible = blocks.size > COLLAPSED_BLOCKS
    val visible = if (expanded || !collapsible) blocks else blocks.take(COLLAPSED_BLOCKS)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(R.string.updates_whats_new, release.versionName),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.animateContentSize().padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                visible.forEachIndexed { index, block -> NotesBlock(block, first = index == 0) }
                if (collapsible) {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(stringResource(if (expanded) R.string.updates_show_less else R.string.updates_show_more))
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesBlock(block: ReleaseNotesBlock, first: Boolean) {
    when (block) {
        is ReleaseNotesBlock.Heading -> Text(
            block.text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = if (first) 0.dp else 8.dp),
        )
        is ReleaseNotesBlock.Bullet -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .padding(top = 8.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant),
            )
            Text(block.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
        is ReleaseNotesBlock.Paragraph -> Text(
            block.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val COLLAPSED_BLOCKS = 8
