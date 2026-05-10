package dev.typetype.android.core.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.compositionLocalOf

val LocalAppSnackbarHost = compositionLocalOf<SnackbarHostState?> { null }
