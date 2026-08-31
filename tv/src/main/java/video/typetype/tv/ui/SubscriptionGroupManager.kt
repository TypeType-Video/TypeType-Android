package video.typetype.tv.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import video.typetype.sdk.core.Subscription
import video.typetype.sdk.core.SubscriptionGroup
import video.typetype.tv.data.TvAppState
import video.typetype.tv.data.TvSubscriptionGroupActions

@Composable
internal fun SubscriptionGroupManager(
    state: TvAppState,
    actions: TvSubscriptionGroupActions,
    onDismiss: () -> Unit,
) {
    var selectedId by rememberSaveable { mutableStateOf(state.selectedSubscriptionGroupId) }
    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<SubscriptionGroup?>(null) }
    var deleting by remember { mutableStateOf<SubscriptionGroup?>(null) }
    val initialFocus = remember { FocusRequester() }
    LaunchedEffect(state.subscriptionGroups, state.selectedSubscriptionGroupId) {
        selectedId = state.selectedSubscriptionGroupId?.takeIf { id ->
            state.subscriptionGroups.any { it.id == id }
        } ?: selectedId?.takeIf { id -> state.subscriptionGroups.any { it.id == id } }
            ?: state.subscriptionGroups.firstOrNull()?.id
    }
    val selectedGroup = state.subscriptionGroups.firstOrNull { it.id == selectedId }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = .78f)),
            contentAlignment = Alignment.Center,
        ) {
            Surface(shape = RoundedCornerShape(18.dp), modifier = Modifier.width(850.dp).height(450.dp)) {
                Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    ManagerHeader(onDismiss)
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                        GroupList(
                            groups = state.subscriptionGroups,
                            selectedId = selectedId,
                            enabled = !state.isActionInProgress,
                            initialFocus = initialFocus,
                            onSelect = {
                                selectedId = it.id
                                actions.select(it.id)
                            },
                            onCreate = { creating = true },
                        )
                        ChannelMemberships(
                            modifier = Modifier.weight(1f),
                            group = selectedGroup,
                            subscriptions = state.subscriptions,
                            enabled = !state.isActionInProgress,
                            onRename = { selectedGroup?.let { renaming = it } },
                            onDelete = { selectedGroup?.let { deleting = it } },
                            onToggle = { subscription -> selectedGroup?.let { actions.toggleChannel(it, subscription) } },
                        )
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) { initialFocus.requestFocus() }
    if (creating) {
        TvTextPrompt("Create group", actionLabel = "Create", onDismiss = { creating = false }) {
            actions.create(it)
            creating = false
        }
    }
    renaming?.let { group ->
        TvTextPrompt(
            title = "Rename group",
            initialValue = group.name,
            actionLabel = "Save",
            onDismiss = { renaming = null },
        ) {
            actions.rename(group, it)
            renaming = null
        }
    }
    deleting?.let { group ->
        TvConfirmDialog(
            title = "Delete ${group.name}?",
            message = "The channels stay subscribed. Only this group is removed.",
            onDismiss = { deleting = null },
            onConfirm = {
                actions.delete(group)
                deleting = null
            },
        )
    }
}

@Composable
private fun ManagerHeader(onDismiss: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Subscription groups", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Shape your home feed without leaving the TV.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = onDismiss) { Text("Done") }
    }
}

@Composable
private fun GroupList(
    groups: List<SubscriptionGroup>,
    selectedId: String?,
    enabled: Boolean,
    initialFocus: FocusRequester,
    onSelect: (SubscriptionGroup) -> Unit,
    onCreate: () -> Unit,
) {
    Column(Modifier.width(250.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Groups", style = MaterialTheme.typography.titleMedium)
        LazyColumn(
            modifier = Modifier.weight(1f).focusRestorer(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(groups, key = { it.id }) { group ->
                val selected = group.id == selectedId
                val firstFocus = group.id == (selectedId ?: groups.firstOrNull()?.id)
                val shape = RoundedCornerShape(10.dp)
                Surface(
                    onClick = { onSelect(group) },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth().then(
                        if (firstFocus) Modifier.focusRequester(initialFocus) else Modifier,
                    ),
                    shape = ClickableSurfaceDefaults.shape(shape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    border = ClickableSurfaceDefaults.border(
                        border = Border(BorderStroke(1.dp, MaterialTheme.colorScheme.border.copy(alpha = .5f)), shape = shape),
                        focusedBorder = Border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), shape = shape),
                    ),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(group.name, modifier = Modifier.weight(1f), maxLines = 1)
                        Text(group.channelCount.toString(), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        Button(
            onClick = onCreate,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().then(
                if (groups.isEmpty()) Modifier.focusRequester(initialFocus) else Modifier,
            ),
        ) { Text("New group") }
    }
}

@Composable
private fun ChannelMemberships(
    modifier: Modifier,
    group: SubscriptionGroup?,
    subscriptions: List<Subscription>,
    enabled: Boolean,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onToggle: (Subscription) -> Unit,
) {
    Column(modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(group?.name ?: "Create your first group", style = MaterialTheme.typography.titleLarge)
                Text(
                    group?.let { channelCountLabel(it.channelCount) } ?: "Organize subscriptions into focused feeds.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (group != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onRename, enabled = enabled) { Text("Rename") }
                    Button(onClick = onDelete, enabled = enabled) { Text("Delete") }
                }
            }
        }
        if (group == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Create a group, then choose the channels it contains.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().focusRestorer(),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                items(subscriptions, key = { it.channelUrl }) { subscription ->
                    MembershipRow(subscription, group.id in subscription.groupIds, enabled) { onToggle(subscription) }
                }
            }
        }
    }
}

@Composable
private fun MembershipRow(subscription: Subscription, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = subscription.avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(38.dp).clip(CircleShape),
            )
            Text(subscription.name, modifier = Modifier.weight(1f), maxLines = 1)
            Text(
                if (selected) "Included" else "Add",
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Unspecified,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private fun channelCountLabel(count: Int): String = if (count == 1) "1 channel" else "$count channels"
