package dev.typetype.android.feature.player.components

import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DescriptionSection(
    title: String,
    viewCount: Long,
    likeCount: Long,
    description: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var pendingUrl by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = buildStatsLine(viewCount, likeCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (expanded && description.isNotBlank()) {
            LinkedText(
                text = description.collapseEmptyLines(),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp,
                ),
                linkColor = MaterialTheme.colorScheme.primary,
                onUrlClick = { url -> pendingUrl = url },
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }

    if (pendingUrl != null) {
        ExternalLinkDialog(
            url = pendingUrl!!,
            onConfirm = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pendingUrl))
                context.startActivity(intent)
                pendingUrl = null
            },
            onDismiss = { pendingUrl = null },
        )
    }
}

@Composable
internal fun LinkedText(
    text: String,
    style: TextStyle,
    linkColor: Color,
    onUrlClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestOnUrlClick = rememberUpdatedState(onUrlClick)
    val annotated = remember(text, linkColor) {
        buildAnnotatedString {
            var lastIndex = 0
            val matcher = Patterns.WEB_URL.matcher(text)
            while (matcher.find()) {
                val url = matcher.group() ?: continue
                val start = matcher.start()
                append(text.substring(lastIndex, start))
                withLink(
                    LinkAnnotation.Url(
                        url = url,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline,
                            ),
                        ),
                        linkInteractionListener = { latestOnUrlClick.value(url) },
                    ),
                ) {
                    append(url)
                }
                lastIndex = matcher.end()
            }
            append(text.substring(lastIndex))
        }
    }
    Text(text = annotated, style = style, modifier = modifier)
}

private fun String.collapseEmptyLines(): String =
    Regex("\\n{3,}").replace(this, "\n\n")

private fun buildStatsLine(viewCount: Long, likeCount: Long): String {
    val parts = buildList {
        add("${formatCompact(viewCount)} views")
        if (likeCount > 0) add("${formatCompact(likeCount)} likes")
    }
    return parts.joinToString(" • ")
}

private fun formatCompact(value: Long): String = when {
    value >= 1_000_000_000 -> "%.1fB".format(value / 1_000_000_000.0)
    value >= 1_000_000 -> "%.1fM".format(value / 1_000_000.0)
    value >= 1_000 -> "%.1fK".format(value / 1_000.0)
    else -> value.toString()
}
