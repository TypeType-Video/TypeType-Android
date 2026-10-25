package dev.typetype.android.feature.settings.recommender

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.domain.recommendations.AlgorithmInfo

@Composable
internal fun AlgorithmCard(algorithm: AlgorithmInfo, selected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected, onClick = null)
                Text(
                    algorithm.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(algorithm.description, style = MaterialTheme.typography.bodyMedium)
            if (selected) {
                GuideLine(R.string.algo_guide_for_you_if, algorithm.forYouIf)
                GuideLine(R.string.algo_guide_you_see, algorithm.youWillSee)
                GuideLine(R.string.algo_guide_good_to_know, algorithm.goodToKnow)
                TraitMeter(R.string.algo_trait_personal, algorithm.traits.personal)
                TraitMeter(R.string.algo_trait_discovery, algorithm.traits.discovery)
                TraitMeter(R.string.algo_trait_variety, algorithm.traits.variety)
            }
        }
    }
}

@Composable
private fun GuideLine(labelRes: Int, text: String) {
    if (text.isBlank()) return
    Column {
        Text(stringResource(labelRes), style = MaterialTheme.typography.labelLarge)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TraitMeter(labelRes: Int, level: Int) {
    val clamped = level.coerceIn(0, MAX_LEVEL)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(labelRes), style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(96.dp))
        LinearProgressIndicator(
            progress = { clamped / MAX_LEVEL.toFloat() },
            modifier = Modifier.weight(1f),
        )
        Text(stringResource(LEVELS[clamped]), style = MaterialTheme.typography.labelMedium)
    }
}

private const val MAX_LEVEL = 3
private val LEVELS = listOf(
    R.string.algo_level_none,
    R.string.algo_level_low,
    R.string.algo_level_medium,
    R.string.algo_level_high,
)
