package dev.typetype.android.feature.settings.recommender

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.typetype.android.R

@Composable
internal fun RecommenderGroup(
    label: String,
    icon: ImageVector? = null,
    help: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var helpShown by rememberSaveable(label) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 40.dp)) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(start = if (icon != null) 8.dp else 0.dp).weight(1f, fill = false),
            )
            if (help != null) InfoButton(label, helpShown) { helpShown = !helpShown }
        }
        AnimatedVisibility(visible = helpShown && help != null) {
            Text(help.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp), content = content)
        }
    }
}

@Composable
internal fun RecommenderRow(
    label: String,
    description: String? = null,
    icon: ImageVector? = null,
    help: String? = null,
    onClick: (() -> Unit)? = null,
    divider: Boolean = true,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    var helpShown by rememberSaveable(label) { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f, fill = false))
                    if (help != null) InfoButton(label, helpShown) { helpShown = !helpShown }
                }
                if (!description.isNullOrBlank()) {
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AnimatedVisibility(visible = helpShown && help != null) {
                    Text(help.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            trailing()
        }
        if (divider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
internal fun Chevron() {
    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun InfoButton(label: String, expanded: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = stringResource(R.string.algo_more_info, label),
            tint = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
internal fun TextAction(text: String, onClick: () -> Unit, enabled: Boolean = true, danger: Boolean = false) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(text, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
    }
}

@Composable
internal fun RecommenderNote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
}

@Composable
internal fun RecommenderNotice(text: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) OutlinedButton(onClick = onAction) { Text(action) }
    }
}

@Composable
internal fun LoadFailed(onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(4.dp)) {
        Text(stringResource(R.string.algo_profile_unavailable), style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.algo_retry)) }
    }
}
