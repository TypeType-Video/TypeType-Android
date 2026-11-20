package dev.typetype.android.feature.player.livechat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.typetype.android.core.ui.share.LocalServerBaseUrl
import dev.typetype.android.core.url.buildImageUrl
import dev.typetype.android.domain.livechat.ChatImage
import dev.typetype.android.domain.livechat.ChatPart
import dev.typetype.android.domain.livechat.LiveChatMessage
import dev.typetype.android.domain.livechat.TwitchChatColor
import dev.typetype.android.domain.livechat.TwitchChatParts
import dev.typetype.android.domain.twitch.TwitchChatAssets

// One Twitch chat line as the web shows it: badges, the name in the viewer's color (made readable on the theme), then
// the message with its emotes inline. Images go through the instance proxy like every other picture in the app; each
// one is read out by its name.
@Composable
internal fun TwitchChatLine(message: LiveChatMessage, assets: TwitchChatAssets, selfLogin: String) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val serverBaseUrl = LocalServerBaseUrl.current
    val parts = remember(message.id, assets) {
        TwitchChatParts.parts(message.text, message.emotes, assets.emotes, selfLogin)
    }
    val badges = remember(message.badges, assets) { message.badges.mapNotNull { assets.badges[it] } }
    val name = message.authorName.orEmpty()
    val nameColor = Color(TwitchChatColor.name(message.color, message.login ?: name, dark))
    val inline = HashMap<String, InlineTextContent>()
    fun image(id: String, images: List<ChatImage>, size: Float) {
        inline[id] = InlineTextContent(Placeholder(size.sp, size.sp, PlaceholderVerticalAlign.TextCenter)) {
            Box {
                images.forEach { picture ->
                    AsyncImage(
                        model = buildImageUrl(serverBaseUrl, picture.url),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
    val text = buildAnnotatedString {
        badges.forEachIndexed { index, badge ->
            val id = "badge-$index"
            image(id, listOf(badge), BADGE_SIZE)
            appendInlineContent(id, badge.name)
            append(' ')
        }
        withStyle(SpanStyle(color = nameColor, fontWeight = FontWeight.SemiBold)) { append(name) }
        append(if (message.action) " " else ": ")
        val body = if (message.action) SpanStyle(color = nameColor, fontStyle = FontStyle.Italic) else SpanStyle()
        withStyle(body) {
            parts.forEachIndexed { index, part ->
                when (part) {
                    is ChatPart.Text -> append(part.text)
                    is ChatPart.Emote -> {
                        val id = "emote-$index"
                        image(id, listOf(part.image) + part.overlays, EMOTE_SIZE)
                        appendInlineContent(id, part.image.name)
                    }
                    is ChatPart.Mention -> withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            background = if (part.self) colors.primary.copy(alpha = 0.2f) else Color.Unspecified,
                        ),
                    ) { append(part.text) }
                    is ChatPart.Link -> withLink(
                        LinkAnnotation.Url(
                            part.href,
                            TextLinkStyles(SpanStyle(color = colors.primary, textDecoration = TextDecoration.Underline)),
                        ),
                    ) { append(part.text) }
                }
            }
        }
    }
    Text(text = text, inlineContent = inline, style = MaterialTheme.typography.bodyMedium)
}

private const val BADGE_SIZE = 18f
private const val EMOTE_SIZE = 28f
