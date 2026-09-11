package dev.typetype.android.feature.player.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle

@Composable
internal fun CommentRichText(
    text: String,
    style: TextStyle,
    linkColor: Color,
    onUrlClick: (String) -> Unit,
    onTimestampClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val latestOnUrlClick = rememberUpdatedState(onUrlClick)
    val latestOnTimestampClick = rememberUpdatedState(onTimestampClick)
    val nodes = remember(text) { parseRichMarkup(text) }
    val linkStyles = remember(linkColor) {
        TextLinkStyles(
            style = SpanStyle(
                color = linkColor,
                textDecoration = TextDecoration.Underline,
            ),
        )
    }
    val highlightBackground = MaterialTheme.colorScheme.surfaceVariant
    val formatStyles = remember(highlightBackground) {
        mapOf(
            RichMarkupFormat.Strong to SpanStyle(fontWeight = FontWeight.Bold),
            RichMarkupFormat.Emphasized to SpanStyle(fontStyle = FontStyle.Italic),
            RichMarkupFormat.Underline to SpanStyle(textDecoration = TextDecoration.Underline),
            RichMarkupFormat.Strikethrough to SpanStyle(textDecoration = TextDecoration.LineThrough),
            RichMarkupFormat.Code to SpanStyle(fontFamily = FontFamily.Monospace),
            RichMarkupFormat.Keyboard to SpanStyle(fontFamily = FontFamily.Monospace),
            RichMarkupFormat.Highlight to SpanStyle(background = highlightBackground),
        )
    }
    val annotated = remember(nodes, linkStyles, formatStyles) {
        buildAnnotatedString {
            appendMarkup(
                nodes = nodes,
                style = SpanStyle(),
                linkStyles = linkStyles,
                formatStyles = formatStyles,
                interactive = true,
                onUrlClick = latestOnUrlClick,
                onTimestampClick = latestOnTimestampClick,
            )
        }
    }
    Text(
        text = annotated,
        style = style,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
    )
}

private fun AnnotatedString.Builder.appendMarkup(
    nodes: List<RichMarkupNode>,
    style: SpanStyle,
    linkStyles: TextLinkStyles,
    formatStyles: Map<RichMarkupFormat, SpanStyle>,
    interactive: Boolean,
    onUrlClick: State<(String) -> Unit>,
    onTimestampClick: State<(Long) -> Unit>,
) {
    nodes.forEach { node ->
        when (node) {
            is RichMarkupNode.Text -> appendText(
                value = node.value,
                style = style,
                linkStyles = linkStyles,
                interactive = interactive,
                onUrlClick = onUrlClick,
                onTimestampClick = onTimestampClick,
            )
            is RichMarkupNode.Break -> append('\n')
            is RichMarkupNode.Link -> withLink(
                LinkAnnotation.Url(
                    url = node.href,
                    styles = linkStyles,
                    linkInteractionListener = { onUrlClick.value(node.href) },
                ),
            ) {
                appendMarkup(
                    nodes = node.children,
                    style = style,
                    linkStyles = linkStyles,
                    formatStyles = formatStyles,
                    interactive = false,
                    onUrlClick = onUrlClick,
                    onTimestampClick = onTimestampClick,
                )
            }
            is RichMarkupNode.Format -> appendMarkup(
                nodes = node.children,
                style = style.merge(formatStyles.getValue(node.format)),
                linkStyles = linkStyles,
                formatStyles = formatStyles,
                interactive = interactive,
                onUrlClick = onUrlClick,
                onTimestampClick = onTimestampClick,
            )
        }
    }
}

private fun AnnotatedString.Builder.appendText(
    value: String,
    style: SpanStyle,
    linkStyles: TextLinkStyles,
    interactive: Boolean,
    onUrlClick: State<(String) -> Unit>,
    onTimestampClick: State<(Long) -> Unit>,
) {
    if (!interactive) {
        withStyle(style) { append(value) }
        return
    }
    withStyle(style) {
        var cursor = 0
        interactiveTextRanges(value).forEach { range ->
            append(value.substring(cursor, range.start))
            when (range) {
                is InteractiveTextRange.Url -> withLink(
                    LinkAnnotation.Url(
                        url = range.value,
                        styles = linkStyles,
                        linkInteractionListener = { onUrlClick.value(range.value) },
                    ),
                ) {
                    append(value.substring(range.start, range.endExclusive))
                }
                is InteractiveTextRange.Timestamp -> withLink(
                    LinkAnnotation.Clickable(
                        tag = range.positionMillis.toString(),
                        styles = linkStyles,
                        linkInteractionListener = { onTimestampClick.value(range.positionMillis) },
                    ),
                ) {
                    append(value.substring(range.start, range.endExclusive))
                }
            }
            cursor = range.endExclusive
        }
        append(value.substring(cursor))
    }
}
