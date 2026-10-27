package dev.typetype.android.feature.settings.updates

internal sealed interface ReleaseNotesBlock {
    data class Heading(val text: String) : ReleaseNotesBlock

    data class Bullet(val text: String) : ReleaseNotesBlock

    data class Paragraph(val text: String) : ReleaseNotesBlock
}

internal fun parseReleaseNotes(markdown: String): List<ReleaseNotesBlock> {
    val blocks = mutableListOf<ReleaseNotesBlock>()
    val paragraph = StringBuilder()
    fun flushParagraph() {
        if (paragraph.isNotBlank()) blocks += ReleaseNotesBlock.Paragraph(paragraph.toString().trim())
        paragraph.clear()
    }
    markdown.lineSequence().map(String::trim).forEach { line ->
        when {
            line.isEmpty() -> flushParagraph()
            line.startsWith("# ") -> flushParagraph()
            line.startsWith("#") -> {
                flushParagraph()
                blocks += ReleaseNotesBlock.Heading(line.trimStart('#').trim().plainText())
            }
            line.startsWith("- ") || line.startsWith("* ") -> {
                flushParagraph()
                blocks += ReleaseNotesBlock.Bullet(line.drop(2).trim().plainText())
            }
            else -> paragraph.append(' ').append(line.plainText())
        }
    }
    flushParagraph()
    return blocks.filterNot { it is ReleaseNotesBlock.Heading && it.text.isBlank() }
}

internal fun List<ReleaseNotesBlock>.dropRepeatedTitle(versionName: String): List<ReleaseNotesBlock> {
    val first = firstOrNull() as? ReleaseNotesBlock.Paragraph ?: return this
    return if (first.text.endsWith(versionName) && first.text.length <= MAX_TITLE_LENGTH) drop(1) else this
}

private fun String.plainText(): String = this
    .replace(LINK, "$1")
    .replace(BOLD, "$1")
    .replace("`", "")

private val LINK = Regex("""\[([^\]]+)]\([^)]*\)""")
private val BOLD = Regex("""\*\*(\S(?:.*?\S)?)\*\*""")
private const val MAX_TITLE_LENGTH = 64
