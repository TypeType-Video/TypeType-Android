package dev.typetype.android.feature.settings.recommender

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.TypeTypeSwitch

internal fun LazyListScope.recommenderHub(state: RecommenderSettingsState, onAction: (RecommenderSettingsAction) -> Unit) {
    item(key = "intro") { RecommenderNote(stringResource(R.string.algo_subtitle)) }
    item(key = "experimental") { ExperimentalCard(state, onAction) }
    item(key = "sections") {
        RecommenderGroup(label = stringResource(R.string.algo_sections_label)) {
            RecommenderSection.entries.forEachIndexed { index, section ->
                RecommenderRow(
                    label = stringResource(section.titleRes),
                    description = stringResource(section.descriptionRes),
                    icon = section.icon,
                    onClick = { onAction(RecommenderSettingsAction.Open(section)) },
                    divider = index < RecommenderSection.entries.lastIndex,
                ) { Chevron() }
            }
        }
    }
}

@Composable
private fun ExperimentalCard(state: RecommenderSettingsState, onAction: (RecommenderSettingsAction) -> Unit) {
    val enabled = state.preferences.enabled
    Surface(
        shape = MaterialTheme.shapes.large,
        color = lerp(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.primary, if (enabled) 0.12f else 0f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 0.8f else 0.35f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .toggleable(value = enabled, role = Role.Switch) { onAction(RecommenderSettingsAction.SetEnabled(it)) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                ) {
                    Text(
                        stringResource(R.string.algo_experimental_badge).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Text(
                    stringResource(R.string.algo_experimental_label),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(stringResource(R.string.algo_experimental_toggle), style = MaterialTheme.typography.bodyMedium)
                AnimatedContent(targetState = enabled, label = "experimental-hint") { on ->
                    val hint = if (on) {
                        state.selectedAlgorithm?.let { stringResource(R.string.algo_experimental_on_with, it.name) }
                            ?: stringResource(R.string.algo_experimental_on_hint)
                    } else {
                        stringResource(R.string.algo_experimental_off_hint)
                    }
                    Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TypeTypeSwitch(checked = enabled, onCheckedChange = null)
        }
    }
}
