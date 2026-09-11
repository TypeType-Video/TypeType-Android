package dev.typetype.android.feature.player.components

import org.junit.Assert.assertEquals
import org.junit.Test

class RichMarkupParserTest {

    @Test
    fun `keeps plain text untouched`() {
        assertEquals(
            listOf<RichMarkupNode>(RichMarkupNode.Text("Just a normal comment")),
            parseRichMarkup("Just a normal comment"),
        )
    }

    @Test
    fun `decodes entities in text`() {
        assertEquals(
            listOf<RichMarkupNode>(RichMarkupNode.Text("\"soft\" 5 > 3 & 2 < 4")),
            parseRichMarkup("&quot;soft&quot; 5 &gt; 3 &amp; 2 &lt; 4"),
        )
    }

    @Test
    fun `turns breaks into break nodes`() {
        assertEquals(
            listOf(
                RichMarkupNode.Text("first"),
                RichMarkupNode.Break,
                RichMarkupNode.Text("second"),
            ),
            parseRichMarkup("first<br>second"),
        )
    }

    @Test
    fun `keeps safe anchors as links with decoded href`() {
        val link = parseRichMarkup("""<a href="https://example.test/watch?v=abc&amp;t=37">watch</a>""")
            .filterIsInstance<RichMarkupNode.Link>()
            .single()

        assertEquals("https://example.test/watch?v=abc&t=37", link.href)
        assertEquals(listOf<RichMarkupNode>(RichMarkupNode.Text("watch")), link.children)
    }

    @Test
    fun `unwraps anchors with unsafe schemes`() {
        assertEquals(
            listOf<RichMarkupNode>(RichMarkupNode.Text("trick")),
            parseRichMarkup("""<a href="javascript:alert(1)">trick</a>"""),
        )
    }

    @Test
    fun `maps formatting tags to formats`() {
        val nodes = parseRichMarkup(
            "<b>bold</b><strong>strong</strong><em>em</em><i>i</i>" +
                "<u>u</u><s>s</s><strike>strike</strike><del>del</del>" +
                "<code>code</code><kbd>kbd</kbd><mark>mark</mark>",
        ).filterIsInstance<RichMarkupNode.Format>()

        assertEquals(
            listOf(
                RichMarkupFormat.Strong,
                RichMarkupFormat.Strong,
                RichMarkupFormat.Emphasized,
                RichMarkupFormat.Emphasized,
                RichMarkupFormat.Underline,
                RichMarkupFormat.Strikethrough,
                RichMarkupFormat.Strikethrough,
                RichMarkupFormat.Strikethrough,
                RichMarkupFormat.Code,
                RichMarkupFormat.Keyboard,
                RichMarkupFormat.Highlight,
            ),
            nodes.map { it.format },
        )
    }

    @Test
    fun `omits dangerous and media tags with their content`() {
        assertEquals(
            listOf<RichMarkupNode>(RichMarkupNode.Text("safe")),
            parseRichMarkup("safe<script>alert(1)</script><img src=x onerror=alert(1)>"),
        )
    }

    @Test
    fun `adds single breaks around block tags`() {
        assertEquals(
            listOf(
                RichMarkupNode.Text("before"),
                RichMarkupNode.Break,
                RichMarkupNode.Text("inside"),
                RichMarkupNode.Break,
                RichMarkupNode.Text("after"),
            ),
            parseRichMarkup("before<div>inside</div>after"),
        )
    }

    @Test
    fun `reparses double escaped markup`() {
        val nodes = parseRichMarkup(
            "&lt;a href=\"https://example.test\"&gt;link&lt;/a&gt;",
        )

        assertEquals(
            listOf<RichMarkupNode>(
                RichMarkupNode.Link("https://example.test", listOf(RichMarkupNode.Text("link"))),
            ),
            nodes,
        )
    }

    @Test
    fun `flattens plain text for truncation`() {
        val source = "look<br><b>bold</b> and " +
            """<a href="https://example.test">a link</a>"""

        assertEquals("look\nbold and a link", richMarkupPlainText(source))
    }

    @Test
    fun `unwraps unknown tags`() {
        assertEquals(
            listOf<RichMarkupNode>(RichMarkupNode.Text("kept")),
            parseRichMarkup("<custom>kept</custom>"),
        )
    }
}
