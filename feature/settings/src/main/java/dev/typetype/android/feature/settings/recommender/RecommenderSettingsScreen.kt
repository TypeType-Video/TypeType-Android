package dev.typetype.android.feature.settings.recommender

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.SettingsSectionHeader
import dev.typetype.android.core.ui.components.SwitchRow
import dev.typetype.android.feature.settings.SettingsDetailTopBar

@Composable
fun RecommenderSettingsRoute(onNavigateBack: () -> Unit) {
    val viewModel: RecommenderSettingsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    RecommenderSettingsScreen(state = state, onAction = viewModel::onAction, onNavigateBack = onNavigateBack)
}

@Composable
internal fun RecommenderSettingsScreen(
    state: RecommenderSettingsState,
    onAction: (RecommenderSettingsAction) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val preferences = state.preferences
    val selected = state.selectedAlgorithm
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsDetailTopBar(title = stringResource(R.string.algo_title), onNavigateBack = onNavigateBack)
            if (state.isLoading || state.isSaving) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.loadFailed) {
                    item {
                        Text(stringResource(R.string.algo_load_failed), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { onAction(RecommenderSettingsAction.Retry) }) {
                            Text(stringResource(R.string.algo_retry))
                        }
                    }
                }
                if (state.saveFailed) {
                    item { Text(stringResource(R.string.algo_save_failed), color = MaterialTheme.colorScheme.error) }
                }
                item {
                    SwitchRow(
                        title = stringResource(R.string.algo_enabled),
                        subtitle = stringResource(R.string.algo_enabled_subtitle),
                        checked = preferences.enabled,
                        onCheckedChange = { onAction(RecommenderSettingsAction.SetEnabled(it)) },
                        enabled = !state.isLoading,
                    )
                }
                val algorithms = state.catalog?.algorithms.orEmpty()
                if (algorithms.isNotEmpty()) {
                    item { SettingsSectionHeader(stringResource(R.string.algo_choose)) }
                    item { Text(stringResource(R.string.algo_guide_intro), style = MaterialTheme.typography.bodyMedium) }
                    items(algorithms, key = { "algorithm-${it.id}" }) { algorithm ->
                        AlgorithmCard(
                            algorithm = algorithm,
                            selected = algorithm.id == selected?.id,
                            onSelect = { onAction(RecommenderSettingsAction.ChooseAlgorithm(algorithm.id)) },
                        )
                    }
                }
                if (selected != null && selected.parameters.isNotEmpty()) {
                    item { SettingsSectionHeader(stringResource(R.string.algo_settings_of, selected.name)) }
                    selected.parameters
                        .sortedByDescending { it.featured }
                        .groupBy { it.group }
                        .forEach { (group, parameters) ->
                            item(key = "group-$group") { ParameterGroupHeader(group) }
                            items(parameters, key = { "parameter-${it.id}" }) { parameter ->
                                ParameterRow(parameter, preferences.parameters[parameter.id]) { value ->
                                    onAction(RecommenderSettingsAction.SetParameter(parameter.id, value))
                                }
                            }
                        }
                    item {
                        TextButton(onClick = { onAction(RecommenderSettingsAction.ResetParameters) }) {
                            Text(stringResource(R.string.algo_reset_all, selected.name))
                        }
                    }
                }
                recommenderInterests(state, onAction)
                recommenderData(state, onAction)
            }
        }
    }
}
