package dev.typetype.android.feature.settings.about

import android.content.Context
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withContext

internal fun buildLibraries(context: Context): Libs {
    return Libs.Builder()
        .withContext(context)
        .build()
}
