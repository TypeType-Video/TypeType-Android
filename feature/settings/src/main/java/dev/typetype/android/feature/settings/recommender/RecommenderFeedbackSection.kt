package dev.typetype.android.feature.settings.recommender

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.SkeletonImage
import dev.typetype.android.domain.recommendations.FeedbackEntry
import java.text.DateFormat
import java.util.Date

internal fun LazyListScope.feedbackSection(state: RecommenderSettingsState, onAction: (RecommenderSettingsAction) -> Unit) {
    item { RecommenderNote(stringResource(R.string.algo_feedback_intro)) }
    item(key = "feedback") {
        RecommenderGroup(label = stringResource(R.string.algo_feedback_label), icon = Icons.Outlined.Forum) {
            when (val loaded = state.feedback) {
                Loadable.Failed -> LoadFailed { onAction(RecommenderSettingsAction.Retry) }
                is Loadable.Loaded -> Column(modifier = Modifier.animateContentSize()) {
                    if (loaded.value.isEmpty()) RecommenderNote(stringResource(R.string.algo_feedback_empty))
                    loaded.value.forEachIndexed { index, entry ->
                        FeedbackRow(entry, state) { onAction(RecommenderSettingsAction.RemoveFeedback(entry.id)) }
                        if (index < loaded.value.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
                else -> RecommenderNote(stringResource(R.string.algo_loading))
            }
        }
    }
}

@Composable
private fun FeedbackRow(entry: FeedbackEntry, state: RecommenderSettingsState, onRemove: () -> Unit) {
    val verdict = stringResource(
        when {
            entry.kind == "video" -> R.string.algo_feedback_not_for_me
            entry.verdict == "good" -> R.string.algo_feedback_good
            else -> R.string.algo_feedback_bad
        },
    )
    val subject = if (entry.kind == "video") {
        entry.title ?: entry.videoUrl
    } else {
        entry.comment
    }
    val details = listOfNotNull(
        entry.channelName.takeIf { entry.kind == "video" },
        entry.reason?.let { feedbackReason(it) },
        state.algorithmName(entry.algorithmId).takeIf { it.isNotBlank() },
        entry.createdAt.takeIf { it > 0 }?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)) },
    ).joinToString(" · ")
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!entry.thumbnailUrl.isNullOrBlank()) {
            SkeletonImage(
                imageUrl = entry.thumbnailUrl.orEmpty(),
                contentDescription = null,
                modifier = Modifier.width(88.dp).aspectRatio(16f / 9f),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(verdict, style = MaterialTheme.typography.bodyLarge)
            if (!subject.isNullOrBlank()) {
                Text(subject, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextAction(stringResource(R.string.algo_feedback_remove), onRemove)
    }
}

@Composable
private fun feedbackReason(reason: String): String? = when (reason) {
    "seen" -> stringResource(R.string.home_algo_reason_seen)
    "channel" -> stringResource(R.string.home_algo_reason_channel)
    "topic" -> stringResource(R.string.home_algo_reason_topic)
    "other" -> stringResource(R.string.home_algo_reason_other)
    "repetitive" -> stringResource(R.string.home_algo_page_repetitive)
    "unrelated" -> stringResource(R.string.home_algo_page_unrelated)
    "old" -> stringResource(R.string.home_algo_page_old)
    "missing" -> stringResource(R.string.home_algo_page_missing)
    else -> null
}
