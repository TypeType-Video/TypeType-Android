package dev.typetype.android.feature.settings.recommender

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.SwitchRow
import dev.typetype.android.domain.recommendations.AlgorithmParameter
import kotlin.math.roundToInt

@Composable
internal fun ParameterGroupHeader(group: String) {
    val (title, help) = groupLabels(group)
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun ParameterRow(parameter: AlgorithmParameter, value: Any?, onChange: (Any) -> Unit) {
    when (parameter) {
        is AlgorithmParameter.Toggle -> SwitchRow(
            title = parameter.label,
            subtitle = parameter.help,
            checked = value as? Boolean ?: parameter.default,
            onCheckedChange = { onChange(it) },
        )
        is AlgorithmParameter.Number -> NumberParameter(parameter, (value as? Number)?.toDouble() ?: parameter.default, onChange)
    }
}

@Composable
private fun NumberParameter(parameter: AlgorithmParameter.Number, current: Double, onChange: (Any) -> Unit) {
    var position by remember(parameter.id, current) { mutableFloatStateOf(current.toFloat()) }
    val intervals = ((parameter.max - parameter.min) / parameter.step).roundToInt()
    val steps = if (intervals in 1..MAX_VISIBLE_STEPS) intervals - 1 else 0
    val shown = formatValue(position.toDouble(), parameter)
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(parameter.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(shown, style = MaterialTheme.typography.labelLarge)
        }
        parameter.help?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = position,
            onValueChange = { position = snap(it.toDouble(), parameter).toFloat() },
            onValueChangeFinished = { onChange(snap(position.toDouble(), parameter)) },
            valueRange = parameter.min.toFloat()..parameter.max.toFloat(),
            steps = steps,
            modifier = Modifier.semantics { stateDescription = shown },
        )
    }
}

private fun snap(value: Double, parameter: AlgorithmParameter.Number): Double {
    val steps = ((value - parameter.min) / parameter.step).roundToInt()
    return (parameter.min + steps * parameter.step).coerceIn(parameter.min, parameter.max)
}

private fun formatValue(value: Double, parameter: AlgorithmParameter.Number): String {
    if (parameter.unit == "%" && parameter.max <= 1.0) return "${(value * 100).roundToInt()} %"
    val number = if (parameter.step >= 1.0) value.roundToInt().toString() else "%.2f".format(value)
    return parameter.unit?.let { "$number $it" } ?: number
}

private fun groupLabels(group: String): Pair<Int, Int> = when (group) {
    "rows" -> R.string.algo_group_rows to R.string.algo_group_rows_help
    "sources" -> R.string.algo_group_sources to R.string.algo_group_sources_help
    "exploration" -> R.string.algo_group_exploration to R.string.algo_group_exploration_help
    "randomness" -> R.string.algo_group_randomness to R.string.algo_group_randomness_help
    "decay" -> R.string.algo_group_decay to R.string.algo_group_decay_help
    "weights" -> R.string.algo_group_weights to R.string.algo_group_weights_help
    else -> R.string.algo_group_other to R.string.algo_group_other_help
}

private const val MAX_VISIBLE_STEPS = 12
