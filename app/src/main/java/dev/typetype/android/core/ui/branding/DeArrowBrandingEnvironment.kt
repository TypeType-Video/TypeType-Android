package dev.typetype.android.core.ui.branding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import dev.typetype.android.domain.branding.DeArrowItem
import dev.typetype.android.domain.branding.DeArrowPreferences
import dev.typetype.android.domain.branding.VideoBranding
import dev.typetype.android.domain.branding.resolveDeArrowBranding

@Immutable
class DeArrowBrandingEnvironment(
    val enabled: Boolean,
    val preferences: DeArrowPreferences,
    private val loader: suspend (String, Long) -> Result<DeArrowItem?>,
) {
    suspend fun load(sourceUrl: String, durationSeconds: Long): Result<DeArrowItem?> =
        loader(sourceUrl, durationSeconds)
}

val LocalDeArrowBranding: ProvidableCompositionLocal<DeArrowBrandingEnvironment> =
    staticCompositionLocalOf {
        DeArrowBrandingEnvironment(
            enabled = false,
            preferences = DeArrowPreferences("original", "original", "accepted"),
            loader = { _, _ -> Result.success(null) },
        )
    }

@Composable
fun rememberVideoBranding(
    sourceUrl: String,
    title: String,
    thumbnailUrl: String,
    durationSeconds: Long,
): VideoBranding {
    val environment = LocalDeArrowBranding.current
    val fallback = VideoBranding(title, thumbnailUrl)
    return produceState(
        initialValue = fallback,
        sourceUrl,
        title,
        thumbnailUrl,
        durationSeconds,
        environment,
    ) {
        value = fallback
        if (environment.enabled) {
            value = environment.load(sourceUrl, durationSeconds).fold(
                onSuccess = { resolveDeArrowBranding(it, fallback, environment.preferences) },
                onFailure = { fallback },
            )
        }
    }.value
}
