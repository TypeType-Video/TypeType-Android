package dev.typetype.android.core.ui.share

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import dev.typetype.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShareChooserSheet(
    serverBaseUrl: String?,
    videoUrl: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val choices = buildShareChoices(serverBaseUrl, videoUrl)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.video_menu_share_sheet_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = stringResource(R.string.video_menu_share_sheet_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            choices.forEach { choice ->
                ShareTargetRow(
                    choice = choice,
                    onClick = {
                        shareUrl(context, choice.url)
                        onDismiss()
                    },
                )
            }
        }
    }
}

@Composable
private fun ShareTargetRow(
    choice: ShareChoice,
    onClick: () -> Unit,
) {
    val isTypeType = choice.target == ShareTarget.TypeType
    val providerName = choice.providerName.orEmpty()
    val iconResource = when {
        isTypeType -> R.drawable.ic_typetype_brand
        providerName == "YouTube" -> R.drawable.ic_service_youtube
        providerName == "NicoNico" -> R.drawable.ic_service_niconico
        else -> R.drawable.ic_service_bilibili
    }
    val iconBackground = when {
        isTypeType -> MaterialTheme.colorScheme.surfaceVariant
        providerName == "YouTube" -> Color(0xFFCC0000)
        providerName == "NicoNico" -> Color(0xFFCC6688)
        else -> Color(0xFFEA4C89)
    }
    val title = if (isTypeType) {
        stringResource(R.string.video_menu_share_target_typetype)
    } else {
        stringResource(R.string.video_menu_share_target_source, providerName)
    }
    val subtitle = if (isTypeType) {
        stringResource(R.string.video_menu_share_target_typetype_description)
    } else {
        stringResource(R.string.video_menu_share_target_source_description, providerName)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconResource),
                contentDescription = null,
                tint = if (isTypeType) Color.Unspecified else Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun shareUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.video_menu_share_chooser)),
    )
}
