package dev.typetype.android.data.network

import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.Response

class ApiErrorParserTest {
    @Test
    fun `keeps structured code status and valid request id`() {
        val response = errorResponse(
            status = 429,
            json = """{"error":"Too many requests","code":"rate_limited"}""",
            requestId = "req-123:retry",
        )

        val error = extractServerError(response)

        assertEquals("Too many requests", error.message)
        assertEquals("rate_limited", error.code)
        assertEquals(429, error.statusCode)
        assertEquals("req-123:retry", error.requestId)
    }

    @Test
    fun `promotes stable legacy error token to code`() {
        val error = extractServerError(
            errorResponse(409, """{"error":"USERNAME_TAKEN"}"""),
        )

        assertEquals("USERNAME_TAKEN", error.message)
        assertEquals("USERNAME_TAKEN", error.code)
    }

    @Test
    fun `does not retain unstructured response body or invalid request id`() {
        val error = extractServerError(
            errorResponse(502, "upstream included private payload", "bad request id"),
        )

        assertEquals("Server request failed", error.message)
        assertNull(error.code)
        assertNull(error.requestId)
    }

    @Test
    fun `successful response passes and failure throws coded exception`() {
        Response.success(Unit).requireSuccessfulResponse()

        val failure = assertThrows(ServerResponseException::class.java) {
            errorResponse(403, """{"error":"Forbidden"}""").requireSuccessfulResponse()
        }

        assertEquals(403, failure.statusCode)
        assertEquals("Forbidden", failure.message)
    }

    private fun errorResponse(
        status: Int,
        json: String,
        requestId: String? = null,
    ): Response<Unit> {
        val raw = okhttp3.Response.Builder()
            .request(Request.Builder().url("https://instance.example/api/test").build())
            .protocol(Protocol.HTTP_1_1)
            .code(status)
            .message("Server failure")
            .apply { requestId?.let { header("X-Request-ID", it) } }
            .build()
        return Response.error(json.toResponseBody(), raw)
    }
}
