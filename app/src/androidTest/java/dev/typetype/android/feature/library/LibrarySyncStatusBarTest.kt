package dev.typetype.android.feature.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LibrarySyncStatusBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun failureShowsSafeRequestIdAndRetryAction() {
        val retried = AtomicBoolean(false)
        composeRule.setContent {
            TypeTypeTheme {
                LibrarySyncStatusBar(
                    isRefreshing = false,
                    lastSuccessfulSyncAtMillis = 1L,
                    errorMessage = "The instance cannot complete this request right now",
                    requestId = "req-123",
                    pendingWriteCount = 0,
                    failedWriteCount = 1,
                    onRetry = { retried.set(true) },
                )
            }
        }

        composeRule.onNodeWithText(
            "The instance cannot complete this request right now",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Request req-123").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed().performClick()
        assertTrue(retried.get())
    }

    @Test
    fun refreshingStateIsVisibleWithoutAnErrorBanner() {
        composeRule.setContent {
            TypeTypeTheme {
                LibrarySyncStatusBar(
                    isRefreshing = true,
                    lastSuccessfulSyncAtMillis = 1L,
                    errorMessage = "ignored old error",
                    requestId = null,
                    pendingWriteCount = 2,
                    failedWriteCount = 1,
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("Refreshing library").assertIsDisplayed()
    }

    @Test
    fun pendingWritesAreVisibleWhenRefreshIsIdle() {
        composeRule.setContent {
            TypeTypeTheme {
                LibrarySyncStatusBar(
                    isRefreshing = false,
                    lastSuccessfulSyncAtMillis = null,
                    errorMessage = null,
                    requestId = null,
                    pendingWriteCount = 2,
                    failedWriteCount = 0,
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("Syncing 2 changes").assertIsDisplayed()
    }
}
