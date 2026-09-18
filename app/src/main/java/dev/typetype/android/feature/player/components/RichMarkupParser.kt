package dev.typetype.android.feature.player.components

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.net.URI

internal enum class RichMarkupFormat {
    Strong,
    Emphasized,
    Underline,
    Strikethrough,
    Code,
    Keyboard,
    Highlight,
}

internal sealed interface RichMarkupNode {
    data class Text(val value: String) : RichMarkupNode

    data object Break : RichMarkupNode

    data class Link(val href: String, val children: List<RichMarkupNode>) : RichMarkupNode

    data class Format(val format: RichMarkupFormat, val children: List<RichMarkupNode>) : RichMarkupNode
}

private val formatTags = setOf(
    "b", "strong", "em", "i", "u", "s", "strike", "del", "code", "kbd", "mark",
)
private val blockTags = setOf(
    "address", "article", "aside", "blockquote", "div", "li", "p", "pre",
)
private val omittedTags = setOf(
    "audio", "base", "embed", "form", "iframe", "img", "link", "meta",
    "object", "script", "style", "svg", "template", "video",
)
private val escapedMarkupPattern = Regex(
    pattern = """&lt;\s*(?:a|br|b|strong|em|i|u|s|p|div)\b""",
    options = setOf(RegexOption.IGNORE_CASE),
)

internal fun parseRichMarkup(source: String): List<RichMarkupNode> {
    val document = Jsoup.parseBodyFragment(source).body()
    val nodes = parseChildren(document.childNodes())
    val markupFree = nodes.all { it is RichMarkupNode.Text }
    if (markupFree && escapedMarkupPattern.containsMatchIn(source)) {
        val decoded = document.text()
        if (decoded != source) {
            return parseChildren(Jsoup.parseBodyFragment(decoded).body().childNodes())
        }
    }
    return nodes
}

internal fun richMarkupPlainText(source: String): String {
    val builder = StringBuilder()
    fun visit(nodes: List<RichMarkupNode>) {
        nodes.forEach { node ->
            when (node) {
                is RichMarkupNode.Text -> builder.append(node.value)
                is RichMarkupNode.Break -> builder.append('\n')
                is RichMarkupNode.Link -> visit(node.children)
                is RichMarkupNode.Format -> visit(node.children)
            }
        }
    }
    visit(parseRichMarkup(source))
    return builder.toString()
}

private fun parseChildren(nodes: List<Node>): List<RichMarkupNode> {
    val result = mutableListOf<RichMarkupNode>()
    nodes.forEach { node ->
        when (node) {
            is TextNode -> if (node.wholeText.isNotEmpty()) {
                result += RichMarkupNode.Text(node.wholeText)
            }
            is Element -> parseElement(node, result)
            else -> Unit
        }
    }
    return result
}

private fun parseElement(element: Element, result: MutableList<RichMarkupNode>) {
    val tag = element.tagName().lowercase()
    if (tag in omittedTags) return
    if (tag == "br") {
        appendBreak(result)
        return
    }
    val children = parseChildren(element.childNodes())
    if (tag == "a") {
        val href = element.attr("href")
        if (isSafeHttpUrl(href)) {
            result += RichMarkupNode.Link(href, children)
        } else {
            result += children
        }
        return
    }
    formatForTag(tag)?.let { format ->
        result += RichMarkupNode.Format(format, children)
        return
    }
    if (tag in blockTags) {
        appendBreak(result)
        result += children
        appendBreak(result)
        return
    }
    result += children
}

private fun formatForTag(tag: String): RichMarkupFormat? = when (tag) {
    "b", "strong" -> RichMarkupFormat.Strong
    "em", "i" -> RichMarkupFormat.Emphasized
    "u" -> RichMarkupFormat.Underline
    "s", "strike", "del" -> RichMarkupFormat.Strikethrough
    "code" -> RichMarkupFormat.Code
    "kbd" -> RichMarkupFormat.Keyboard
    "mark" -> RichMarkupFormat.Highlight
    else -> null
}

private fun appendBreak(nodes: MutableList<RichMarkupNode>) {
    if (nodes.isNotEmpty() && nodes.last() != RichMarkupNode.Break) {
        nodes += RichMarkupNode.Break
    }
}

private fun isSafeHttpUrl(value: String): Boolean = runCatching {
    val scheme = URI(value).scheme ?: return@runCatching false
    scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)
}.getOrDefault(false)
