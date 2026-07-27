package dev.typetype.android.feature.player.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink

internal sealed interface InteractiveTextRange {
    val start: Int
    val endExclusive: Int

    data class Url(
        override val start: Int,
        override val endExclusive: Int,
        val value: String,
    ) : InteractiveTextRange

    data class Timestamp(
        override val start: Int,
        override val endExclusive: Int,
        val positionMillis: Long,
    ) : InteractiveTextRange
}

private val urlPattern = Regex(
    pattern = """(?i)\b(?:https?://|www\.)[^\s<]+""",
)
private val timestampPattern = Regex(
    pattern = """(?<![\p{L}\p{N}:])(?:(\d{1,3}):)?(\d{1,3}):([0-5]\d)(?![:\d])""",
)
private val trailingUrlPunctuation = setOf('.', ',', ';', ':', '!', '?', ')', ']', '}')

internal fun interactiveTextRanges(text: String): List<InteractiveTextRange> {
    val urls = urlPattern.findAll(text).mapNotNull { match ->
        val value = match.value.trimEnd { it in trailingUrlPunctuation }
        value.takeIf(String::isNotEmpty)?.let {
            InteractiveTextRange.Url(
                start = match.range.first,
                endExclusive = match.range.first + value.length,
                value = normalizeExternalUrl(value),
            )
        }
    }.toList()
    val timestamps = timestampPattern.findAll(text).mapNotNull { match ->
        if (urls.any { match.range.first < it.endExclusive && match.range.last >= it.start }) {
            return@mapNotNull null
        }
        val hours = match.groupValues[1].takeIf(String::isNotEmpty)?.toLongOrNull()
        val minutes = match.groupValues[2].toLongOrNull() ?: return@mapNotNull null
        val seconds = match.groupValues[3].toLongOrNull() ?: return@mapNotNull null
        if (hours != null && minutes > 59) return@mapNotNull null
        val totalSeconds = (hours ?: 0L) * 3600L + minutes * 60L + seconds
        InteractiveTextRange.Timestamp(
            start = match.range.first,
            endExclusive = match.range.last + 1,
            positionMillis = totalSeconds * 1000L,
        )
    }
    return (urls + timestamps).sortedBy(InteractiveTextRange::start)
}

internal fun normalizeExternalUrl(url: String): String =
    if (url.startsWith("http://", true) || url.startsWith("https://", true)) {
        url
    } else {
        "https://$url"
    }

@Composable
internal fun LinkedText(
    text: String,
    style: TextStyle,
    linkColor: Color,
    onUrlClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onTimestampClick: (Long) -> Unit = {},
) {
    val latestOnUrlClick = rememberUpdatedState(onUrlClick)
    val latestOnTimestampClick = rememberUpdatedState(onTimestampClick)
    val ranges = remember(text) { interactiveTextRanges(text) }
    val linkStyle = remember(linkColor) {
        TextLinkStyles(
            style = SpanStyle(
                color = linkColor,
                textDecoration = TextDecoration.Underline,
            ),
        )
    }
    val annotated = buildAnnotatedString {
        var cursor = 0
        ranges.forEach { range ->
            append(text.substring(cursor, range.start))
            when (range) {
                is InteractiveTextRange.Url -> withLink(
                    LinkAnnotation.Url(
                        url = range.value,
                        styles = linkStyle,
                        linkInteractionListener = {
                            latestOnUrlClick.value(range.value)
                        },
                    ),
                ) {
                    append(text.substring(range.start, range.endExclusive))
                }
                is InteractiveTextRange.Timestamp -> withLink(
                    LinkAnnotation.Clickable(
                        tag = range.positionMillis.toString(),
                        styles = linkStyle,
                        linkInteractionListener = {
                            latestOnTimestampClick.value(range.positionMillis)
                        },
                    ),
                ) {
                    append(text.substring(range.start, range.endExclusive))
                }
            }
            cursor = range.endExclusive
        }
        append(text.substring(cursor))
    }
    Text(text = annotated, style = style, modifier = modifier)
}
