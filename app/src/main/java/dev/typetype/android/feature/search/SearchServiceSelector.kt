package dev.typetype.android.feature.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.typetype.android.R

@Immutable
private data class SearchService(
    val id: Int,
    val labelRes: Int,
    val iconRes: Int,
    val brandColor: Color,
)

private val SEARCH_SERVICES = listOf(
    SearchService(
        id = 0,
        labelRes = R.string.settings_default_service_youtube,
        iconRes = R.drawable.ic_service_youtube,
        brandColor = Color(0xFFFF0000),
    ),
    SearchService(
        id = 6,
        labelRes = R.string.settings_default_service_niconico,
        iconRes = R.drawable.ic_service_niconico,
        brandColor = Color(0xFF231815),
    ),
    SearchService(
        id = 5,
        labelRes = R.string.settings_default_service_bilibili,
        iconRes = R.drawable.ic_service_bilibili,
        brandColor = Color(0xFF00A1D6),
    ),
)

@Composable
internal fun SearchServiceSelector(
    service: Int,
    onServiceSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SEARCH_SERVICES.forEach { candidate ->
            val label = stringResource(candidate.labelRes)
            FilterChip(
                selected = service == candidate.id,
                onClick = { onServiceSelect(candidate.id) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(candidate.iconRes),
                        contentDescription = null,
                        tint = candidate.brandColor,
                        modifier = Modifier.size(18.dp),
                    )
                },
                label = { Text(label) },
            )
        }
    }
}
