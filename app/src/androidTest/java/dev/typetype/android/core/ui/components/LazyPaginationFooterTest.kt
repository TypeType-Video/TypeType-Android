package dev.typetype.android.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LazyPaginationFooterTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun eachNewContinuationLoadsExactlyOnePage() {
        var continuation by mutableStateOf<String?>("page-two")
        val loadCount = AtomicInteger()

        composeRule.setContent {
            MaterialTheme {
                LazyPaginationFooter(
                    continuationKey = continuation,
                    isLoading = false,
                    hasError = false,
                    onLoadMore = loadCount::incrementAndGet,
                )
            }
        }

        composeRule.waitUntil { loadCount.get() == 1 }
        composeRule.runOnUiThread { continuation = "page-three" }
        composeRule.waitUntil { loadCount.get() == 2 }
        composeRule.runOnUiThread { continuation = null }
        composeRule.waitForIdle()

        assertEquals(2, loadCount.get())
    }

    @Test
    fun failedContinuationWaitsForAnExplicitRetry() {
        val loadCount = AtomicInteger()

        composeRule.setContent {
            MaterialTheme {
                LazyPaginationFooter(
                    continuationKey = "page-two",
                    isLoading = false,
                    hasError = true,
                    onLoadMore = loadCount::incrementAndGet,
                )
            }
        }

        composeRule.waitForIdle()
        assertEquals(0, loadCount.get())

        val retryLabel = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getString(dev.typetype.android.R.string.search_load_more_retry)
        composeRule.onNodeWithText(retryLabel).performClick()

        assertEquals(1, loadCount.get())
    }
}
