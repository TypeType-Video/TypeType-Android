package dev.typetype.android.data.version

import dev.typetype.android.data.network.dto.ComponentVersionDto
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Response

class ComponentVersionPolicyTest {
    @Test
    fun `keeps successful components when another component is unavailable`() = runBlocking {
        val available = loadComponentVersion { Response.success(versionDto()) }
        val unavailable = loadComponentVersion { throw IOException("offline") }

        assertEquals("1.3.1", available?.version)
        assertNull(unavailable)
    }

    @Test
    fun `rejects unsuccessful component responses`() = runBlocking {
        val response = Response.error<ComponentVersionDto>(
            503,
            "".toResponseBody(),
        )

        assertNull(loadComponentVersion { response })
    }

    private fun versionDto() = ComponentVersionDto(
        service = "server",
        version = "1.3.1",
        revision = "revision",
        shortRevision = "rev",
        buildTime = "2026-08-05T10:54:40Z",
    )
}
