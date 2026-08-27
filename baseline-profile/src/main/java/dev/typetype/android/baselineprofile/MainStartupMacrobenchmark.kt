package dev.typetype.android.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainStartupMacrobenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    @OptIn(ExperimentalMetricApi::class)
    fun startup() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(
            StartupTimingMetric(),
            MemoryUsageMetric(
                MemoryUsageMetric.Mode.Max,
                listOf(
                    MemoryUsageMetric.SubMetric.RssAnon,
                    MemoryUsageMetric.SubMetric.RssFile,
                    MemoryUsageMetric.SubMetric.RssShmem,
                ),
            ),
        ),
        compilationMode = CompilationMode.Partial(),
        startupMode = StartupMode.COLD,
        iterations = 5,
        setupBlock = { pressHome() },
    ) {
        pressHome()
        startActivityAndWait()
    }

    private companion object {
        const val PACKAGE_NAME = "dev.typetype.android"
    }
}
