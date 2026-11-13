package dev.typetype.android.core.ui.error

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberUserErrorMapper(): UserErrorMapper {
    val context = LocalContext.current.applicationContext
    return remember(context) { UserErrorMapper(context) }
}
