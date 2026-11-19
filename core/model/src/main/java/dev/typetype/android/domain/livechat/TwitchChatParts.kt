package dev.typetype.android.domain.livechat

data class ChatEmoteRange(val id: String, val start: Int, val end: Int)

data class ChatImage(val name: String, val url: String)

data class ChatEmote(val name: String, val url: String, val zeroWidth: Boolean = false)

sealed interface ChatPart {
    data class Text(val text: String) : ChatPart
    data class Emote(val image: ChatImage, val overlays: List<ChatImage> = emptyList()) : ChatPart
    data class Mention(val text: String, val self: Boolean) : ChatPart
    data class Link(val text: String, val href: String) : ChatPart
}

// Same split as the web chat: Twitch's own emotes come as code point ranges, BTTV/FFZ/7TV ones as whole words, and a
// zero-width emote sits on top of the emote before it.
object TwitchChatParts {
    private const val TWITCH_EMOTE_BASE = "https://static-cdn.jtvnw.net/emoticons/v2/"
    private val LINK = Regex("""^(?:https?://)?(?:[a-z0-9-]+\.)+[a-z]{2,}(?::\d{2,5})?(?:[/?#]\S*)?$""", RegexOption.IGNORE_CASE)
    private val TRAILING = Regex("""[),.!?:;'"\]]+$""")
    private val SPACES = Regex("""(\s+)""")
    private val BARE_TLDS = (
        "com net org info biz xyz io co app dev me tv gg ly to sh so ai fm am " +
            "video live stream games game fun art blog club online site store shop tech link page news wiki social media " +
            "be fr de uk es it nl eu ru jp cn kr br ca us au ch at se no dk fi pl pt cz ie in mx ar"
        ).split(" ").toSet()

    fun twitchEmote(id: String, name: String): ChatImage =
        ChatImage(name, "$TWITCH_EMOTE_BASE$id/default/dark/2.0")

    fun parts(
        text: String,
        native: List<ChatEmoteRange> = emptyList(),
        emotes: Map<String, ChatEmote> = emptyMap(),
        selfLogin: String = "",
    ): List<ChatPart> {
        val points = text.codePoints().toArray()
        fun slice(from: Int, to: Int) = String(points, from, to - from)
        val ranges = native.filter { it.start >= 0 && it.end < points.size && it.start <= it.end }.sortedBy { it.start }
        val builder = Builder(emotes, selfLogin.lowercase())
        var cursor = 0
        for (range in ranges) {
            if (range.start < cursor) continue
            builder.segment(slice(cursor, range.start))
            builder.parts += ChatPart.Emote(twitchEmote(range.id, slice(range.start, range.end + 1)))
            cursor = range.end + 1
        }
        builder.segment(slice(cursor, points.size))
        return builder.parts
    }

    fun linkHref(word: String): String? {
        if (!LINK.matches(word)) return null
        val schemed = word.startsWith("http://", ignoreCase = true) || word.startsWith("https://", ignoreCase = true)
        val host = word.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#')
            .substringBefore(':').lowercase()
        if (!schemed && !host.startsWith("www.") && host.substringAfterLast('.') !in BARE_TLDS) return null
        return if (schemed) word else "https://$word"
    }

    private class Builder(val emotes: Map<String, ChatEmote>, val self: String) {
        val parts = mutableListOf<ChatPart>()

        fun segment(text: String) {
            var index = 0
            for (match in SPACES.findAll(text)) {
                word(text.substring(index, match.range.first))
                pushText(match.value)
                index = match.range.last + 1
            }
            word(text.substring(index))
        }

        private fun word(word: String) {
            if (word.isEmpty()) return
            val emote = emotes[word]
            if (emote != null) {
                val image = ChatImage(emote.name, emote.url)
                val base = if (emote.zeroWidth) lastEmoteIndex() else -1
                if (base >= 0) {
                    if (parts.lastOrNull() is ChatPart.Text) parts.removeAt(parts.lastIndex)
                    val target = parts[base] as ChatPart.Emote
                    parts[base] = target.copy(overlays = target.overlays + image)
                } else {
                    parts += ChatPart.Emote(image)
                }
                return
            }
            if (word.startsWith("@") && word.length > 1) {
                val name = word.drop(1).replace(TRAILING, "")
                parts += ChatPart.Mention("@$name", name.lowercase() == self)
                pushText(word.drop(name.length + 1))
                return
            }
            val trimmed = word.replace(TRAILING, "")
            val href = linkHref(trimmed)
            if (href != null) {
                parts += ChatPart.Link(trimmed, href)
                pushText(word.drop(trimmed.length))
                return
            }
            pushText(word)
        }

        private fun lastEmoteIndex(): Int {
            val last = parts.lastIndex
            val index = if (last >= 0 && (parts[last] as? ChatPart.Text)?.text?.isBlank() == true) last - 1 else last
            return if (index >= 0 && parts[index] is ChatPart.Emote) index else -1
        }

        private fun pushText(text: String) {
            if (text.isEmpty()) return
            val last = parts.lastOrNull()
            if (last is ChatPart.Text) parts[parts.lastIndex] = ChatPart.Text(last.text + text) else parts += ChatPart.Text(text)
        }
    }
}
