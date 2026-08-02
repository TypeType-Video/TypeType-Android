package dev.typetype.android.feature.player.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.stream.SponsorAction
import dev.typetype.android.domain.stream.SponsorBlockSegment
import dev.typetype.android.domain.stream.SponsorCategory
import org.junit.Rule
import org.junit.Test

class SponsorBlockSkipNoticeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun automaticSkipNamesItsSponsorBlockCategory() {
        composeRule.setContent {
            TypeTypeTheme {
                SponsorBlockSkipNotice(
                    segment = SponsorBlockSegment(
                        startMs = 494_618L,
                        endMs = 618_714L,
                        category = SponsorCategory.Outro,
                        action = SponsorAction.Skip,
                    ),
                )
            }
        }

        composeRule.onNodeWithText("Skipped automatically").assertIsDisplayed()
        composeRule.onNodeWithText("Outro").assertIsDisplayed()
    }

    @Test
    fun noticeStaysHiddenInPictureInPicture() {
        composeRule.setContent {
            TypeTypeTheme {
                SponsorBlockSkipNotice(
                    segment = SponsorBlockSegment(
                        startMs = 10_000L,
                        endMs = 20_000L,
                        category = SponsorCategory.Sponsor,
                        action = SponsorAction.Skip,
                    ),
                    visible = false,
                )
            }
        }

        composeRule.onNodeWithText("Skipped automatically").assertIsNotDisplayed()
    }
}
