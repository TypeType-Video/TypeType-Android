package dev.typetype.android.data.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OidcTransactionStoreTest {
    @Test
    fun transactionSurvivesStoreRecreationAndRejectsAnotherState() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val initial = OidcTransactionStore(context)
        initial.clear(SERVER_ID)
        initial.start(SERVER_ID, EXPECTED_STATE)
        assertTrue(initial.hasPending(SERVER_ID))

        val recreated = OidcTransactionStore(context)
        recreated.requireMatches(SERVER_ID, EXPECTED_STATE)
        assertThrows(IllegalStateException::class.java) {
            recreated.requireMatches(SERVER_ID, "another-state")
        }
        recreated.clear(SERVER_ID)
    }

    private companion object {
        const val SERVER_ID = "oidc-test-instance"
        const val EXPECTED_STATE = "signed-state"
    }
}
