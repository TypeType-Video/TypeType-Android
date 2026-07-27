package dev.typetype.android.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.gif.repeatCount
import coil3.request.ImageRequest
import dev.typetype.android.R

@Composable
fun AnimatedLoader(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    val ctx = LocalPlatformContext.current
    AsyncImage(
        model = ImageRequest.Builder(ctx)
            .data(R.raw.loader)
            .repeatCount(Int.MAX_VALUE)
            .build(),
        contentDescription = stringResource(R.string.state_loading),
        modifier = modifier.size(size),
    )
}

@Composable
fun FullScreenLoader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedLoader(size = 120.dp)
    }
}

@Composable
fun StreamErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    illustrationRes: Int = R.raw.error_cat,
    countryCode: String? = null,
    requestId: String? = null,
    onRetry: (() -> Unit)? = null,
    retryLabel: String? = null,
    onBack: (() -> Unit)? = null,
) {
    val ctx = LocalPlatformContext.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(ctx)
                    .data(illustrationRes)
                    .repeatCount(Int.MAX_VALUE)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(20.dp)),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.2).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (countryCode != null) {
                        Text(
                            text = countryCodeToFlagEmoji(countryCode),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                requestId?.let { RequestIdRow(requestId = it) }
            }
            if (onRetry != null || onBack != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onRetry != null) {
                        ActionPill(
                            label = retryLabel ?: stringResource(R.string.state_retry),
                            background = MaterialTheme.colorScheme.onBackground,
                            contentColor = MaterialTheme.colorScheme.background,
                            onClick = onRetry,
                        )
                    }
                    if (onBack != null) {
                        ActionPill(
                            label = stringResource(R.string.state_go_back),
                            background = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            onClick = onBack,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedError(
    message: String,
    modifier: Modifier = Modifier,
    requestId: String? = null,
    onRetry: (() -> Unit)? = null,
) {
    StreamErrorState(
        title = stringResource(R.string.state_something_went_wrong),
        message = message,
        modifier = modifier,
        illustrationRes = R.raw.error_cat,
        requestId = requestId,
        onRetry = onRetry,
    )
}

@Composable
private fun ActionPill(
    label: String,
    background: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
            color = contentColor,
        )
    }
}

private fun countryCodeToFlagEmoji(code: String): String {
    if (code.length != 2) return ""
    val a = code.uppercase()
    val first = 0x1F1E6 + (a[0].code - 'A'.code)
    val second = 0x1F1E6 + (a[1].code - 'A'.code)
    return String(Character.toChars(first)) + String(Character.toChars(second))
}
