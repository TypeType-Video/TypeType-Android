package dev.typetype.android.feature.settings.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.RequestIdRow
import dev.typetype.android.core.ui.components.ProfileAvatar
import dev.typetype.android.core.ui.components.resolveProfileAvatarUrl
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.domain.account.Account
import dev.typetype.android.domain.server.Server

@Composable
fun InstanceAccountsCard(
    server: Server,
    accounts: List<Account>,
    activeScope: AccountScope?,
    busyAccountId: String?,
    onSelect: (String, String) -> Unit,
    onForget: (String, String) -> Unit,
    onSignIn: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = server.displayName,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = server.baseUrl,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (accounts.isEmpty()) {
            Text(
                text = stringResource(R.string.accounts_none_saved),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            accounts.forEach { account ->
                val selected = activeScope == AccountScope(server.id, account.id)
                AccountRow(
                    account = account,
                    avatarUrl = resolveProfileAvatarUrl(
                        serverBaseUrl = server.baseUrl,
                        avatarUrl = account.avatarUrl,
                        avatarType = account.avatarType,
                        avatarCode = account.avatarCode,
                        fallbackSeed = "${account.id}:${account.publicUsername}",
                    ),
                    selected = selected,
                    busy = busyAccountId == account.id,
                    onSelect = { onSelect(server.id, account.id) },
                    onForget = { onForget(server.id, account.id) },
                )
            }
        }
        OutlinedButton(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.accounts_sign_in_another))
        }
    }
}

@Composable
private fun AccountRow(
    account: Account,
    avatarUrl: String?,
    selected: Boolean,
    busy: Boolean,
    onSelect: () -> Unit,
    onForget: () -> Unit,
) {
    val displayName = account.publicUsername?.takeIf { it.isNotBlank() }
        ?: if (account.isGuest) stringResource(R.string.accounts_guest) else account.id
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            )
            .clickable(enabled = !selected && !busy, onClick = onSelect)
            .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatar(
            imageUrl = avatarUrl,
            fallbackLetter = displayName,
            contentDescription = displayName,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (account.isGuest) stringResource(R.string.accounts_guest)
                else account.role.orEmpty().ifBlank { stringResource(R.string.accounts_member) },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when {
            busy -> CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            selected -> Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.accounts_active))
            else -> IconButton(onClick = onForget) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = stringResource(R.string.accounts_forget),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
fun AccountSettingsError(message: String, requestId: String?) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
        requestId?.let { RequestIdRow(requestId = it) }
    }
}
