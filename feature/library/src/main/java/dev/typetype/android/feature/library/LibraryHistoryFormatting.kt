package dev.typetype.android.feature.library

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun formatDate(millis: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))
