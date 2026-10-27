package dev.typetype.android.feature.settings.recommender

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.domain.recommendations.AlgorithmInfo

internal fun LazyListScope.guideSection(state: RecommenderSettingsState, onAction: (RecommenderSettingsAction) -> Unit) {
    item { RecommenderNote(stringResource(R.string.algo_guide_intro)) }
    items(state.algorithms, key = { "guide-${it.id}" }) { algorithm ->
        GuideCard(algorithm, current = algorithm.id == state.selectedAlgorithm?.id) {
            onAction(RecommenderSettingsAction.ChooseAlgorithm(algorithm.id, announce = true))
        }
    }
}

@Composable
private fun GuideCard(algorithm: AlgorithmInfo, current: Boolean, onChoose: () -> Unit) {
    var details by rememberSaveable(algorithm.id) { mutableStateOf(false) }
    val border by animateColorAsState(
        if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        label = "guide-border",
    )
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(if (current) 2.dp else 1.dp, border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    algorithmIcon(algorithm),
                    contentDescription = null,
                    tint = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    algorithm.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f),
                )
                if (current) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text(stringResource(R.string.algo_guide_current), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    OutlinedButton(onClick = onChoose) { Text(stringResource(R.string.algo_guide_choose)) }
                }
            }
            if (algorithm.forYouIf.isNotBlank()) Text(algorithm.forYouIf, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TraitMeter(R.string.algo_trait_personal, algorithm.traits.personal, Modifier.weight(1f))
                TraitMeter(R.string.algo_trait_discovery, algorithm.traits.discovery, Modifier.weight(1f))
                TraitMeter(R.string.algo_trait_variety, algorithm.traits.variety, Modifier.weight(1f))
            }
            val turn by animateFloatAsState(if (details) 180f else 0f, label = "guide-chevron")
            TextButton(onClick = { details = !details }, contentPadding = PaddingValues(end = 8.dp)) {
                Text(stringResource(if (details) R.string.algo_guide_less else R.string.algo_guide_details))
                Icon(Icons.Outlined.ExpandMore, contentDescription = null, modifier = Modifier.rotate(turn))
            }
            AnimatedVisibility(visible = details) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GuideEntry(Icons.Outlined.Visibility, R.string.algo_guide_you_see, algorithm.youWillSee)
                    GuideEntry(Icons.Outlined.Lightbulb, R.string.algo_guide_good_to_know, algorithm.goodToKnow)
                    val reads = algorithm.uses.map { dataLabel(it) }.joinToString(", ")
                    GuideEntry(Icons.Outlined.Storage, R.string.algo_guide_data, reads.ifEmpty { stringResource(R.string.algo_uses_nothing) })
                }
            }
        }
    }
}

@Composable
private fun GuideEntry(icon: ImageVector, term: Int, text: String) {
    if (text.isBlank()) return
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(term), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TraitMeter(label: Int, level: Int, modifier: Modifier) {
    val clamped = level.coerceIn(0, LEVEL_LABELS.lastIndex)
    val levelText = stringResource(LEVEL_LABELS[clamped])
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    Column(modifier = modifier.semantics { stateDescription = levelText }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..LEVEL_LABELS.lastIndex).forEach { step ->
                val color by animateColorAsState(
                    if (entered && step <= clamped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                    animationSpec = tween(durationMillis = 400, delayMillis = step * 90),
                    label = "trait",
                )
                Box(Modifier.width(20.dp).height(6.dp).background(color, RoundedCornerShape(3.dp)))
            }
        }
        Text(levelText, style = MaterialTheme.typography.bodyMedium)
    }
}
