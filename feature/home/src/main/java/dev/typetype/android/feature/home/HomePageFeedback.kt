package dev.typetype.android.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.domain.recommendations.PageRefusalReason

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomePageFeedback(
    verdict: PageVerdict,
    reason: PageRefusalReason?,
    onRate: (Boolean) -> Unit,
    onReason: (PageRefusalReason) -> Unit,
    onSend: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (verdict) {
            PageVerdict.Thanked -> Text(
                stringResource(R.string.home_algo_thanks),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PageVerdict.Unasked -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.home_algo_question), style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = { onRate(true) }) {
                    Icon(Icons.Outlined.ThumbUp, contentDescription = stringResource(R.string.home_algo_page_good))
                }
                IconButton(onClick = { onRate(false) }) {
                    Icon(Icons.Outlined.ThumbDown, contentDescription = stringResource(R.string.home_algo_page_bad))
                }
            }
            PageVerdict.AskingWhy -> {
                var comment by rememberSaveable { mutableStateOf("") }
                Text(stringResource(R.string.home_algo_page_why), style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PageRefusalReason.entries.forEach { option ->
                        FilterChip(
                            selected = reason == option,
                            onClick = { onReason(option) },
                            label = { Text(stringResource(option.label())) },
                        )
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it.take(MAX_COMMENT) },
                    label = { Text(stringResource(R.string.home_algo_comment)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = { onSend(comment) }) { Text(stringResource(R.string.home_algo_send)) }
            }
        }
    }
}

private fun PageRefusalReason.label(): Int = when (this) {
    PageRefusalReason.Repetitive -> R.string.home_algo_page_repetitive
    PageRefusalReason.Unrelated -> R.string.home_algo_page_unrelated
    PageRefusalReason.Old -> R.string.home_algo_page_old
    PageRefusalReason.Missing -> R.string.home_algo_page_missing
}

private const val MAX_COMMENT = 1000
