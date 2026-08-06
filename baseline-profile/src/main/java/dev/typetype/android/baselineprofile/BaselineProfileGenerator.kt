package dev.typetype.android.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() = baselineProfileRule.collect(
        packageName = PACKAGE_NAME,
        includeInStartupProfile = true,
        strictStability = true,
        filterPredicate = STARTUP_RULE::containsMatchIn,
    ) {
        startActivityAndWait()
    }

    private companion object {
        const val PACKAGE_NAME = "dev.typetype.android"
        val STARTUP_RULE = Regex(
            "^[HSP]*Ldev/typetype/android/" +
                "(MainActivity|MainViewModel|TypeTypeApp|AppShellKt|AppNavHostKt|" +
                "SetupNavigationKt|" +
                "Hilt_MainActivity|Hilt_TypeTypeApp)(\\$|;)",
        )
    }
}
