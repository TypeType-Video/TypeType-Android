package dev.typetype.android.baselineprofile

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShortsMacrobenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startup() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        startupMode = StartupMode.COLD,
        iterations = 5,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait(benchmarkIntent())
    }

    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun verticalPaging() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(
            FrameTimingMetric(),
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
        setupBlock = { startActivityAndWait(benchmarkIntent()) },
    ) {
        repeat(6) {
            device.swipe(
                device.displayWidth / 2,
                device.displayHeight * 3 / 4,
                device.displayWidth / 2,
                device.displayHeight / 4,
                10,
            )
            device.waitForIdle()
        }
    }

    private fun benchmarkIntent() = Intent().setClassName(PACKAGE_NAME, ACTIVITY_NAME)

    private companion object {
        const val PACKAGE_NAME = "dev.typetype.android"
        const val ACTIVITY_NAME = "dev.typetype.android.benchmark.ShortsBenchmarkActivity"
    }
}
