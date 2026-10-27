package dev.typetype.android.feature.settings.recommender

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.TypeTypeSwitch
import dev.typetype.android.domain.recommendations.AlgorithmParameter
import kotlin.math.roundToInt

@Composable
internal fun ParameterRow(
    parameter: AlgorithmParameter,
    value: Any,
    showDefault: Boolean = false,
    onRestore: (() -> Unit)? = null,
    onChange: (Any) -> Unit,
) {
    val changed = !parameter.isDefault(value)
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        when (parameter) {
            is AlgorithmParameter.Toggle -> Row(verticalAlignment = Alignment.CenterVertically) {
                ParameterTitle(parameter, showDefault, Modifier.weight(1f))
                TypeTypeSwitch(checked = value as? Boolean ?: parameter.default, onCheckedChange = { onChange(it) })
            }
            is AlgorithmParameter.Number -> NumberParameter(parameter, (value as? Number)?.toDouble() ?: parameter.default, showDefault, onChange)
        }
        AnimatedVisibility(visible = changed && onRestore != null) {
            TextAction(stringResource(R.string.algo_restore), { onRestore?.invoke() })
        }
    }
}

@Composable
private fun ParameterTitle(parameter: AlgorithmParameter, showDefault: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(parameter.label, style = MaterialTheme.typography.bodyLarge)
        parameter.help?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (showDefault) {
            Text(
                stringResource(R.string.algo_default, formatParameter(parameter, parameter.defaultValue)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NumberParameter(parameter: AlgorithmParameter.Number, current: Double, showDefault: Boolean, onChange: (Any) -> Unit) {
    var position by remember(parameter.id, current) { mutableFloatStateOf(current.toFloat()) }
    val shown = formatParameter(parameter, position.toDouble())
    Row(verticalAlignment = Alignment.Top) {
        ParameterTitle(parameter, showDefault, Modifier.weight(1f))
        Text(shown, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 12.dp))
    }
    Slider(
        value = position,
        onValueChange = { position = snap(it.toDouble(), parameter).toFloat() },
        onValueChangeFinished = { onChange(snap(position.toDouble(), parameter)) },
        valueRange = parameter.min.toFloat()..parameter.max.toFloat(),
        modifier = Modifier.semantics { stateDescription = shown },
    )
}

private fun snap(value: Double, parameter: AlgorithmParameter.Number): Double {
    val steps = ((value - parameter.min) / parameter.step).roundToInt()
    val snapped = (parameter.min + steps * parameter.step).coerceIn(parameter.min, parameter.max)
    return (snapped * PRECISION).roundToInt() / PRECISION
}

private const val PRECISION = 10_000.0
