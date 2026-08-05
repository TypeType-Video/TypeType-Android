package dev.typetype.android.feature.settings.player

import dev.typetype.android.domain.usersettings.SponsorBlockMode
import dev.typetype.android.domain.usersettings.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SponsorBlockSettingsTest {
    @Test
    fun `global mode applies to every known category`() {
        val updated = UserSettings().withSponsorBlockMode(SponsorBlockMode.MarkOnly)

        assertEquals(SponsorBlockMode.MarkOnly, updated.sponsorBlockMode)
        assertTrue(updated.sponsorBlockCategoryActions.isNotEmpty())
        assertTrue(updated.sponsorBlockCategoryActions.values.all {
            it == SponsorBlockMode.MarkOnly
        })
    }

    @Test
    fun `advanced options only update their matching field`() {
        val initial = UserSettings(
            sponsorBlockShowChapters = false,
            sponsorBlockMuteInsteadOfSkip = false,
        )

        val chapters = initial.withSponsorBlockOption(SponsorBlockOption.ShowChapters, true)
        val muted = chapters.withSponsorBlockOption(SponsorBlockOption.MuteInsteadOfSkip, true)

        assertTrue(muted.sponsorBlockShowChapters)
        assertTrue(muted.sponsorBlockMuteInsteadOfSkip)
        assertTrue(muted.sponsorBlockShowCurrentSegment)
        assertEquals(initial.sponsorBlockMode, muted.sponsorBlockMode)
    }
}
