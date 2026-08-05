package dev.typetype.android.feature.player.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlaybackDanmakuOptionsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun supportedVideoCanToggleBulletComments() {
        val enabled = AtomicBoolean(false)
        show(enabled = false) { enabled.set(it) }

        composeRule.onNode(hasText("Show bullet comments") and hasClickAction()).performClick()

        assertTrue(enabled.get())
    }

    @Test
    fun enabledBulletCommentsExposeSpeedAndSize() {
        show(enabled = true)

        composeRule.onNodeWithText("Bullet comment speed").assertIsDisplayed()
        composeRule.onNodeWithText("Bullet comment size").assertIsDisplayed()
    }

    private fun show(
        enabled: Boolean,
        onDanmakuChange: (Boolean) -> Unit = {},
    ) {
        composeRule.setContent {
            TypeTypeTheme {
                PlaybackOptionsMainPage(
                    codecLabel = "Recommended",
                    qualityLabel = "Auto",
                    captionsLabel = "Off",
                    audioLabel = "Default",
                    speedLabel = "1x",
                    resizeLabel = "Fit",
                    audioOnlyEnabled = false,
                    audioOnlyChanging = false,
                    showAudioOnly = false,
                    showDanmaku = true,
                    danmakuEnabled = enabled,
                    danmakuSpeedLabel = "1x",
                    danmakuSizeLabel = "100%",
                    danmakuLoading = false,
                    danmakuLoadFailed = false,
                    onOpenCodec = {},
                    onOpenQuality = {},
                    onOpenCaptions = {},
                    onOpenAudio = {},
                    onOpenSpeed = {},
                    onOpenResize = {},
                    onAudioOnlyChange = {},
                    onDanmakuChange = onDanmakuChange,
                    onOpenDanmakuSpeed = {},
                    onOpenDanmakuSize = {},
                )
            }
        }
    }
}
