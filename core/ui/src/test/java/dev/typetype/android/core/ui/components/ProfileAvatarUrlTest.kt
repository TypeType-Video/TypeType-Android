package dev.typetype.android.core.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileAvatarUrlTest {

    @Test
    fun `server absolute path keeps api base path`() {
        val url = resolveProfileAvatarUrl(
            serverBaseUrl = "https://beta.typetype.video/api",
            avatarUrl = "/avatar/custom/user/version",
            avatarType = "custom",
            avatarCode = null,
            fallbackSeed = "user",
        )

        assertEquals(
            "https://beta.typetype.video/api/avatar/custom/user/version",
            url,
        )
    }

    @Test
    fun `absolute image url remains unchanged`() {
        val url = resolveProfileAvatarUrl(
            serverBaseUrl = "https://beta.typetype.video/api",
            avatarUrl = "https://images.example/avatar.gif",
            avatarType = "custom",
            avatarCode = null,
            fallbackSeed = "user",
        )

        assertEquals("https://images.example/avatar.gif", url)
    }
}
