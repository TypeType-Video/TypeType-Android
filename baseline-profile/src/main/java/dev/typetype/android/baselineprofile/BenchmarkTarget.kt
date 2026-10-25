package dev.typetype.android.baselineprofile

import android.content.Intent

internal const val TARGET_PACKAGE = "dev.typetype.android.benchmark"

internal fun benchmarkActivity(simpleName: String): Intent =
    Intent().setClassName(TARGET_PACKAGE, "dev.typetype.android.benchmark.$simpleName")
