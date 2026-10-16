package dev.typetype.android.feature.home

import dev.typetype.android.domain.server.Server
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelServerChangeTest {
    @Test
    fun refreshesOnlyWhenTheActiveInstanceChanges() {
        val original = server("instance-a")

        assertTrue(shouldRefreshHomeForServer(null, original))
        assertFalse(shouldRefreshHomeForServer(original.id, original.copy(version = "1.10.0")))
        assertFalse(shouldRefreshHomeForServer(original.id, null))
        assertTrue(shouldRefreshHomeForServer(null, original))
        assertTrue(shouldRefreshHomeForServer(original.id, server("instance-b")))
    }

    private fun server(id: String) = Server(
        id = id,
        baseUrl = "https://example.test/api/",
        displayName = "TypeType",
        addedAt = 1L,
    )
}
