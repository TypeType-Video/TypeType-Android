package dev.typetype.android.feature.settings.updates

import dev.typetype.android.feature.settings.updates.ReleaseNotesBlock.Bullet
import dev.typetype.android.feature.settings.updates.ReleaseNotesBlock.Heading
import dev.typetype.android.feature.settings.updates.ReleaseNotesBlock.Paragraph
import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseNotesBlocksTest {
    @Test
    fun stableNotesBecomeSectionsBulletsAndParagraphs() {
        val notes = """
            # TypeType for Android 1.11.0

            TypeType for Android 1.11.0 brings
            personalized recommendations.

            ## Playback and recovery

            - Resume playback after a long pause.
            * Keep retrying after network loss.
        """.trimIndent()

        assertEquals(
            listOf(
                Paragraph("TypeType for Android 1.11.0 brings personalized recommendations."),
                Heading("Playback and recovery"),
                Bullet("Resume playback after a long pause."),
                Bullet("Keep retrying after network loss."),
            ),
            parseReleaseNotes(notes),
        )
    }

    @Test
    fun inlineMarkdownIsShownAsPlainText() {
        val notes = "- **Full changelog:** see [the compare view](https://example.com) and `p256dh` keys"

        assertEquals(
            listOf(Bullet("Full changelog: see the compare view and p256dh keys")),
            parseReleaseNotes(notes),
        )
    }

    @Test
    fun underscoresInsideWordsAreKept() {
        assertEquals(listOf(Paragraph("retry_fresh_session")), parseReleaseNotes("retry_fresh_session"))
    }

    @Test
    fun aBetaTitleLineRepeatingTheVersionIsDropped() {
        val blocks = parseReleaseNotes("TypeType Android 1.11.0-beta.9\n\n- Recommendations have their own entry.")

        assertEquals(
            listOf(Bullet("Recommendations have their own entry.")),
            blocks.dropRepeatedTitle("1.11.0-beta.9"),
        )
    }

    @Test
    fun anIntroductionMentioningTheVersionIsKept() {
        val blocks = listOf(Paragraph("TypeType for Android 1.11.0 brings recommendations and a lot more for 1.11.0"))

        assertEquals(blocks, blocks.dropRepeatedTitle("1.11.0"))
    }
}
