package dev.typetype.android.core.ui.error

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.typetype.android.R
import dev.typetype.android.data.network.ServerError
import dev.typetype.android.data.network.ServerResponseException
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserErrorMapperAndroidTest {
    private lateinit var context: Context
    private lateinit var mapper: UserErrorMapper

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mapper = UserErrorMapper(context)
    }

    @Test
    fun mapsServerFamiliesToLocalizedActions() {
        assertMapped(401, R.string.error_sign_in_again)
        assertMapped(403, R.string.error_permission_denied)
        assertMapped(404, R.string.error_content_unavailable)
        assertMapped(409, R.string.error_conflict)
        assertMapped(429, R.string.error_rate_limited)
        assertMapped(503, R.string.error_server_unavailable)
    }

    @Test
    fun keepsInitialAuthenticationRejectionDistinctFromExpiredSession() {
        val message = mapper.authenticationMessage(
            failure = serverFailure(401),
            fallbackRes = R.string.login_failed,
            rejectedRes = R.string.login_credentials_rejected,
        )

        assertEquals(context.getString(R.string.login_credentials_rejected), message)
    }

    @Test
    fun mapsTransportFailureAndPreservesOperationFallback() {
        assertEquals(
            context.getString(R.string.error_network_unavailable),
            mapper.message(IOException("offline"), R.string.search_failed),
        )
        assertEquals(
            context.getString(R.string.search_failed),
            mapper.message(IllegalStateException("unexpected"), R.string.search_failed),
        )
    }

    private fun assertMapped(status: Int, expectedResource: Int) {
        assertEquals(
            context.getString(expectedResource),
            mapper.message(serverFailure(status), R.string.snackbar_action_failed),
        )
    }

    private fun serverFailure(status: Int) = ServerResponseException(
        ServerError("Server request failed", null, status),
    )
}
