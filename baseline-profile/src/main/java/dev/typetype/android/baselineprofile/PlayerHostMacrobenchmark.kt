package dev.typetype.android.baselineprofile

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerHostMacrobenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun playerMorph() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(
            MemoryUsageMetric(
                mode = MemoryUsageMetric.Mode.Max,
                subMetrics = listOf(
                    MemoryUsageMetric.SubMetric.HeapSize,
                    MemoryUsageMetric.SubMetric.RssAnon,
                ),
            ),
        ),
        compilationMode = CompilationMode.Partial(),
        iterations = 5,
        setupBlock = {
            killProcess()
            startActivityAndWait(benchmarkIntent())
        },
    ) {
        repeat(3) {
            swipeDown(durationMs = 700, steps = 45)
            swipeUp(durationMs = 700, steps = 45)
            swipeDown(durationMs = 120, steps = 8)
            swipeUp(durationMs = 120, steps = 8)
        }
    }

    private fun MacrobenchmarkScope.swipeDown(durationMs: Int, steps: Int) {
        device.swipe(
            device.displayWidth / 2,
            device.displayHeight / 4,
            device.displayWidth / 2,
            device.displayHeight * 4 / 5,
            steps,
        )
        device.waitForIdle((durationMs + 800).toLong())
    }

    private fun MacrobenchmarkScope.swipeUp(durationMs: Int, steps: Int) {
        device.swipe(
            device.displayWidth / 2,
            device.displayHeight * 9 / 10,
            device.displayWidth / 2,
            device.displayHeight / 4,
            steps,
        )
        device.waitForIdle((durationMs + 800).toLong())
    }

    private fun benchmarkIntent() = Intent().setClassName(PACKAGE_NAME, ACTIVITY_NAME)

    private companion object {
        const val PACKAGE_NAME = "dev.typetype.android"
        const val ACTIVITY_NAME = "dev.typetype.android.benchmark.PlayerHostBenchmarkActivity"
    }
}
