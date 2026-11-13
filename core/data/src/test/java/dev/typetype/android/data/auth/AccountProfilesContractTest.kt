package dev.typetype.android.data.auth

import dev.typetype.android.data.network.dto.AccountProfilesResponse
import dev.typetype.android.data.network.dto.ProfileSwitchResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountProfilesContractTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `profiles list keeps the active and default profile`() {
        val body = json.decodeFromString<AccountProfilesResponse>(
            """{"profiles":[{"id":"a","name":"Main","isActive":true,"isDefault":true,"lastUsedAt":1},
               {"id":"b","name":"Kids","isActive":false,"isDefault":false,"lastUsedAt":2,"avatarType":"emoji"}],
               "activeProfileId":"a","defaultProfileId":"a"}""",
        )

        assertEquals(listOf("Main", "Kids"), body.profiles.map { it.name })
        assertEquals("a", body.activeProfileId)
        assertTrue(body.profiles.first().isDefault)
    }

    @Test
    fun `switch response carries the new session token`() {
        val body = json.decodeFromString<ProfileSwitchResponse>(
            """{"accessToken":"t","profile":{"id":"b","name":"Kids","isActive":true,"isDefault":false,"lastUsedAt":3}}""",
        )

        assertEquals("t", body.accessToken)
        assertEquals("b", body.profile.id)
    }
}
