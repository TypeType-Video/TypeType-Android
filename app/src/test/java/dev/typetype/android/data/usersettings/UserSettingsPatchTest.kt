package dev.typetype.android.data.usersettings

import dev.typetype.android.data.network.dto.UserSettingsDto
import dev.typetype.android.domain.usersettings.CaptionStyles
import dev.typetype.android.domain.usersettings.DEFAULT_SPONSOR_BLOCK_CATEGORY_ACTIONS
import dev.typetype.android.domain.usersettings.SponsorBlockMode
import dev.typetype.android.domain.usersettings.UserSettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSettingsPatchTest {
    @Test
    fun `unchanged settings produce an empty patch`() {
        val settings = UserSettings(disableWatchHistory = true)

        assertTrue(settings.patchFrom(settings).isEmpty())
    }

    @Test
    fun `history tracking toggle only patches the server privacy field`() {
        val patch = UserSettings(disableWatchHistory = true).patchFrom(UserSettings())

        assertEquals(setOf("disableWatchHistory"), patch.keys)
        assertTrue(requireNotNull(patch["disableWatchHistory"]).jsonPrimitive.boolean)
    }

    @Test
    fun `player update does not resend unrelated settings`() {
        val previous = UserSettings(
            defaultService = 6,
            autoplay = false,
            disableWatchHistory = true,
        )

        val patch = previous.copy(defaultService = 0).patchFrom(previous)

        assertEquals(setOf("defaultService"), patch.keys)
        assertEquals(0, requireNotNull(patch["defaultService"]).jsonPrimitive.int)
    }

    @Test
    fun `playlist countdown preference uses the server field`() {
        val patch = UserSettings(skipPlaylistAutoplayScreen = true).patchFrom(UserSettings())

        assertEquals(setOf("skipPlaylistAutoplayScreen"), patch.keys)
        assertTrue(requireNotNull(patch["skipPlaylistAutoplayScreen"]).jsonPrimitive.boolean)
    }

    @Test
    fun `server privacy value maps into the domain policy`() {
        val dto = JSON.decodeFromString<UserSettingsDto>(
            """
                {
                  "disableWatchHistory": true,
                  "hideHomeRecommendations": true,
                  "hideContinueWatching": true,
                  "hideComments": true,
                  "skipPlaylistAutoplayScreen": true
                }
            """.trimIndent(),
        )

        assertTrue(dto.toDomain().disableWatchHistory)
        assertTrue(dto.toDomain().hideHomeRecommendations)
        assertTrue(dto.toDomain().hideContinueWatching)
        assertTrue(dto.toDomain().hideComments)
        assertTrue(dto.toDomain().autoplay)
        assertTrue(dto.toDomain().skipPlaylistAutoplayScreen)
    }

    @Test
    fun `old server settings keep subscription live control unsupported`() {
        val settings = JSON.decodeFromString<UserSettingsDto>("{}").toDomain()

        assertTrue(!settings.supportsHideSubscriptionLiveStreams)
        assertTrue(!settings.hideSubscriptionLiveStreams)
    }

    @Test
    fun `new server settings expose and patch only subscription live visibility`() {
        val previous = JSON.decodeFromString<UserSettingsDto>(
            """{"hideSubscriptionLiveStreams":false}""",
        ).toDomain()
        val changed = previous.copy(hideSubscriptionLiveStreams = true)

        assertTrue(previous.supportsHideSubscriptionLiveStreams)
        assertEquals(setOf("hideSubscriptionLiveStreams"), changed.patchFrom(previous).keys)
        assertTrue(
            requireNotNull(changed.patchFrom(previous)["hideSubscriptionLiveStreams"])
                .jsonPrimitive.boolean,
        )
    }

    @Test
    fun `unsupported subscription live field is never sent to an old server`() {
        val previous = UserSettings()
        val changed = previous.copy(hideSubscriptionLiveStreams = true)

        assertTrue(changed.patchFrom(previous).isEmpty())
    }

    @Test
    fun `complete server settings response maps without dropping preferences`() {
        val dto = JSON.decodeFromString<UserSettingsDto>(COMPLETE_SETTINGS_JSON)

        assertEquals(
            UserSettings(
                defaultService = 6,
                defaultQuality = "2160p",
                defaultPlaybackSpeed = 1.5,
                defaultLandingPage = "subscriptions",
                autoplay = false,
                skipPlaylistAutoplayScreen = true,
                volume = 0.5,
                muted = true,
                subtitlesEnabled = true,
                defaultSubtitleLanguage = "fr",
                defaultAudioLanguage = "en",
                captionStyles = CaptionStyles(
                    fontFamily = "mono",
                    fontSize = "large",
                    textColor = "#ffffff",
                    textOpacity = "90",
                    textShadow = "raised",
                    textBackground = "#000000",
                    textBackgroundOpacity = "50",
                    displayBackground = "#111111",
                    displayBackgroundOpacity = "25",
                ),
                preferOriginalLanguage = true,
                enableHighQualityPlayback = true,
                sponsorBlockMode = SponsorBlockMode.MarkOnly,
                sponsorBlockCategoryActions = DEFAULT_SPONSOR_BLOCK_CATEGORY_ACTIONS +
                    mapOf(
                        "sponsor" to SponsorBlockMode.Disabled,
                        "custom" to SponsorBlockMode.MarkOnly,
                    ),
                sponsorBlockMinimumDuration = 3,
                sponsorBlockShowCurrentSegment = false,
                sponsorBlockShowChapters = true,
                sponsorBlockShowFullVideoLabels = false,
                sponsorBlockManualSkipOnFullVideo = false,
                sponsorBlockSkipNonMusicOnlyOnMusicVideos = true,
                sponsorBlockMuteInsteadOfSkip = true,
                hideHomeRecommendations = true,
                hideContinueWatching = true,
                hideRelatedVideos = true,
                hideComments = true,
                hideShorts = true,
                disableWatchHistory = true,
                deArrowEnabled = true,
                deArrowTitleMode = "original",
                deArrowThumbnailMode = "random",
                deArrowTrustMode = "all",
                accessMode = "allow_list",
            ),
            dto.toDomain(),
        )
    }

    @Test
    fun `complete settings change creates a typed server patch`() {
        val previous = UserSettings()
        val changed = JSON.decodeFromString<UserSettingsDto>(COMPLETE_SETTINGS_JSON).toDomain()

        val patch = changed.patchFrom(previous)

        assertEquals(COMPLETE_PATCH_KEYS, patch.keys)
        assertEquals(1.5, requireNotNull(patch["defaultPlaybackSpeed"]).jsonPrimitive.double, 0.0)
        assertEquals(
            "disabled",
            requireNotNull(patch["sponsorBlockCategoryActions"])
                .jsonObject
                .getValue("sponsor")
                .jsonPrimitive
                .content,
        )
        assertEquals(
            "#000000",
            requireNotNull(patch["captionStyles"])
                .jsonObject
                .getValue("textBg")
                .jsonPrimitive
                .content,
        )
    }

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true }

        val COMPLETE_PATCH_KEYS = setOf(
            "defaultService",
            "defaultQuality",
            "defaultPlaybackSpeed",
            "defaultLandingPage",
            "autoplay",
            "skipPlaylistAutoplayScreen",
            "volume",
            "muted",
            "subtitlesEnabled",
            "defaultSubtitleLanguage",
            "defaultAudioLanguage",
            "captionStyles",
            "preferOriginalLanguage",
            "enableHighQualityPlayback",
            "sponsorBlockMode",
            "sponsorBlockCategoryActions",
            "sponsorBlockMinimumDuration",
            "sponsorBlockShowCurrentSegment",
            "sponsorBlockShowChapters",
            "sponsorBlockShowFullVideoLabels",
            "sponsorBlockManualSkipOnFullVideo",
            "sponsorBlockSkipNonMusicOnlyOnMusicVideos",
            "sponsorBlockMuteInsteadOfSkip",
            "hideHomeRecommendations",
            "hideContinueWatching",
            "hideRelatedVideos",
            "hideComments",
            "hideShorts",
            "disableWatchHistory",
            "deArrowEnabled",
            "deArrowTitleMode",
            "deArrowThumbnailMode",
            "deArrowTrustMode",
            "accessMode",
        )

        val COMPLETE_SETTINGS_JSON =
            """
            {
              "defaultService": 6,
              "defaultQuality": "2160p",
              "defaultPlaybackSpeed": 1.5,
              "defaultLandingPage": "subscriptions",
              "autoplay": false,
              "skipPlaylistAutoplayScreen": true,
              "volume": 0.5,
              "muted": true,
              "subtitlesEnabled": true,
              "defaultSubtitleLanguage": "fr",
              "defaultAudioLanguage": "en",
              "captionStyles": {
                "fontFamily": "mono",
                "fontSize": "large",
                "textColor": "#ffffff",
                "textOpacity": "90",
                "textShadow": "raised",
                "textBg": "#000000",
                "textBgOpacity": "50",
                "displayBg": "#111111",
                "displayBgOpacity": "25"
              },
              "preferOriginalLanguage": true,
              "enableHighQualityPlayback": true,
              "sponsorBlockMode": "mark_only",
              "sponsorBlockCategoryActions": {
                "sponsor": "disabled",
                "custom": "mark_only"
              },
              "sponsorBlockMinimumDuration": 3,
              "sponsorBlockShowCurrentSegment": false,
              "sponsorBlockShowChapters": true,
              "sponsorBlockShowFullVideoLabels": false,
              "sponsorBlockManualSkipOnFullVideo": false,
              "sponsorBlockSkipNonMusicOnlyOnMusicVideos": true,
              "sponsorBlockMuteInsteadOfSkip": true,
              "hideHomeRecommendations": true,
              "hideContinueWatching": true,
              "hideRelatedVideos": true,
              "hideComments": true,
              "hideShorts": true,
              "disableWatchHistory": true,
              "deArrowEnabled": true,
              "deArrowTitleMode": "original",
              "deArrowThumbnailMode": "random",
              "deArrowTrustMode": "all",
              "accessMode": "allow_list"
            }
            """.trimIndent()
    }
}
