package dev.typetype.android.feature.settings.imports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.domain.imports.PortabilityDirection
import dev.typetype.android.domain.imports.PortabilityDuplicatePolicy

@Composable
internal fun EmptyImportForm(
    state: PortabilityUiState,
    onFormatSelected: (dev.typetype.android.domain.imports.PortabilityFormat) -> Unit,
    onChooseFile: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        PortabilityFormatPicker(
            labelRes = R.string.portability_import_from,
            formats = state.formats,
            selected = state.selectedFormat,
            onSelect = onFormatSelected,
        )
        state.selectedFormat?.let { format ->
            ImportGuide(format.format)
            DropZone(
                extension = format.defaultExtension,
                onChooseFile = onChooseFile,
            )
        }
    }
}

@Composable
private fun DropZone(
    extension: String,
    onChooseFile: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(176.dp)
            .clickable(role = Role.Button, onClick = onChooseFile),
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Filled.UploadFile, contentDescription = null)
            Text(
                text = stringResource(R.string.portability_choose_or_drop),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = ".$extension",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ImportGuide(format: String) {
    val uriHandler = LocalUriHandler.current
    val name = portabilityFormatName(format)
    val descriptionRes = when (format) {
        "typetype" -> R.string.portability_guide_typetype_description
        "youtube-takeout" -> R.string.portability_guide_takeout_description
        "opml" -> R.string.portability_guide_opml_description
        else -> null
    }
    val description = descriptionRes?.let { resource -> stringResource(resource) }
        ?: stringResource(R.string.portability_guide_generic_description, name)
    val steps = when (format) {
        "typetype" -> listOf(
            R.string.portability_guide_typetype_step_one,
            R.string.portability_guide_typetype_step_two,
            R.string.portability_guide_typetype_step_three,
        )
        "youtube-takeout" -> listOf(
            R.string.portability_guide_takeout_step_one,
            R.string.portability_guide_takeout_step_two,
            R.string.portability_guide_takeout_step_three,
        )
        "opml" -> listOf(
            R.string.portability_guide_opml_step_one,
            R.string.portability_guide_opml_step_two,
            R.string.portability_guide_opml_step_three,
        )
        else -> listOf(
            R.string.portability_guide_generic_open,
            R.string.portability_guide_generic_download,
            R.string.portability_guide_generic_drop,
        )
    }.map { resource ->
        if (resource == R.string.portability_guide_generic_open) {
            stringResource(resource, name)
        } else {
            stringResource(resource)
        }
    }
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.portability_get_backup, name), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (format == "youtube-takeout") {
                    PanelButton(
                        text = stringResource(R.string.portability_guide_takeout_action),
                        enabled = true,
                        onClick = { uriHandler.openUri("https://takeout.google.com/settings/takeout/custom/youtube,my_activity?dest=mail&frequency=once") },
                    )
                }
            }
            HorizontalDivider()
            steps.forEachIndexed { index, step ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("$index", style = MaterialTheme.typography.labelLarge)
                    Text(step, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
internal fun ReadyImportPreview(
    state: PortabilityUiState,
    onCategoryToggled: (dev.typetype.android.domain.imports.TypeTypeBackupCategory) -> Unit,
    onPolicySelected: (PortabilityDuplicatePolicy) -> Unit,
    onApplyImport: () -> Unit,
    onResetJob: () -> Unit,
) {
    val preview = requireNotNull(state.job?.preview)
    val detectionFormat = preview.detection?.format
        ?.let { wireName -> state.formats.firstOrNull { it.format == wireName } }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PortabilityFormatIcon(preview.detection?.format.orEmpty())
            Column(modifier = Modifier.weight(1f)) {
                Text(portabilityFormatName(preview.detection?.format.orEmpty()))
                Text(
                    "${preview.duplicates} ${stringResource(R.string.portability_duplicate_records)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(onClick = onResetJob, shape = MaterialTheme.shapes.extraSmall) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.height(14.dp))
                    Text(stringResource(R.string.portability_different_file), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        PortabilityCategoryGrid(
            format = detectionFormat,
            direction = PortabilityDirection.Import,
            selected = state.selectedCategories,
            counts = preview.counts,
            onToggle = onCategoryToggled,
        )
        if (preview.issues.isNotEmpty()) {
            Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.portability_compatibility_notes), style = MaterialTheme.typography.labelMedium)
                    preview.issues.forEach { issue ->
                        Text("${issue.message} (${issue.count})", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                listOf(PortabilityDuplicatePolicy.Skip, PortabilityDuplicatePolicy.Replace).forEach { policy ->
                    val active = policy == state.duplicatePolicy
                    Surface(
                        modifier = Modifier.clickable(role = Role.Button) { onPolicySelected(policy) },
                        color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
                        contentColor = if (active) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = MaterialTheme.shapes.extraSmall,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Text(
                            stringResource(
                                if (policy == PortabilityDuplicatePolicy.Skip) {
                                    R.string.portability_skip
                                } else {
                                    R.string.portability_replace
                                },
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
            PanelButton(
                stringResource(R.string.portability_import_selected),
                enabled = state.selectedCategories.isNotEmpty() && !state.isApplying,
                emphasized = true,
                onClick = onApplyImport,
            )
        }
    }
}

@Composable
internal fun PanelButton(
    text: String,
    enabled: Boolean = true,
    emphasized: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.height(36.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        shape = MaterialTheme.shapes.extraSmall,
        color = if (emphasized) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
        contentColor = if (emphasized) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}
