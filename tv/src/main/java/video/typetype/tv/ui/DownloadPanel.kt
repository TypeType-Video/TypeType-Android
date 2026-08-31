package video.typetype.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import video.typetype.sdk.core.DownloadJob
import video.typetype.sdk.core.DownloadJobStatus
import video.typetype.tv.data.TvDownloadKind
import video.typetype.tv.data.TvDownloadOption

@Composable
internal fun DownloadPanel(
    options: List<TvDownloadOption>,
    job: DownloadJob?,
    isSaving: Boolean,
    message: String?,
    error: String?,
    onStart: (TvDownloadOption) -> Unit,
    onCancel: () -> Unit,
    onRetryArtifact: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    val actionFocus = remember { FocusRequester() }
    var kind by remember { mutableStateOf(TvDownloadKind.VIDEO) }
    var selectedId by remember(options, kind) {
        mutableStateOf(options.firstOrNull { it.kind == kind && it.recommended }?.id ?: options.firstOrNull { it.kind == kind }?.id)
    }
    val filtered = options.filter { it.kind == kind }
    val selected = filtered.firstOrNull { it.id == selectedId } ?: filtered.firstOrNull()
    BackHandler(onBack = onDismiss)
    LaunchedEffect(job?.id, job?.status, message, error) {
        if (job == null) firstFocus.requestFocus() else actionFocus.requestFocus()
    }
    Surface(
        modifier = Modifier.width(620.dp).fillMaxHeight(),
        colors = androidx.tv.material3.SurfaceDefaults.colors(containerColor = Color(0xFF111316)),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 30.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { DownloadHeader(onDismiss) }
            if (job == null || message != null) {
                item {
                    DownloadModeSelector(
                        selected = kind,
                        enabled = job == null,
                        firstFocus = firstFocus,
                        onSelect = {
                            kind = it
                            selectedId = options.firstOrNull { option -> option.kind == it && option.recommended }?.id
                                ?: options.firstOrNull { option -> option.kind == it }?.id
                        },
                    )
                }
            }
            if (job == null) {
                items(filtered, key = TvDownloadOption::id) { option ->
                    DownloadOptionRow(option, option.id == selected?.id) { selectedId = option.id }
                }
                item {
                    Button(
                        onClick = { selected?.let(onStart) },
                        enabled = selected != null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = downloadActionColors(),
                    ) { DownloadButtonContent("Start download") }
                }
            } else {
                item { DownloadProgress(job, isSaving, message, error) }
                item {
                    DownloadJobActions(
                        job, isSaving, message, error, actionFocus,
                        onCancel, onRetryArtifact, onClear, onDismiss,
                    )
                }
            }
            if (job == null && error != null) item { DownloadError(error) }
            if (filtered.isEmpty() && job == null) item {
                Text("No downloadable format was returned by this stream.", color = Color.White.copy(alpha = .7f))
            }
        }
    }
}

@Composable
private fun DownloadHeader(onDismiss: () -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Download", color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Choose exactly what the TypeType server should prepare.", color = Color.White.copy(alpha = .62f))
        }
        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.colors(
                containerColor = Color(0xFF292C31),
                contentColor = Color.White,
                focusedContainerColor = MaterialTheme.colorScheme.primary,
                focusedContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(Icons.Default.Close, contentDescription = null)
            Spacer(Modifier.width(7.dp))
            Text("Close")
        }
    }
}

@Composable
private fun DownloadModeSelector(
    selected: TvDownloadKind,
    enabled: Boolean,
    firstFocus: FocusRequester,
    onSelect: (TvDownloadKind) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TvDownloadKind.entries.forEach { kind ->
            Button(
                onClick = { onSelect(kind) },
                enabled = enabled,
                modifier = Modifier.weight(1f).then(
                    if (kind == TvDownloadKind.VIDEO) Modifier.focusRequester(firstFocus) else Modifier,
                ),
                colors = ButtonDefaults.colors(
                    containerColor = if (kind == selected) MaterialTheme.colorScheme.primary.copy(alpha = .34f) else Color(0xFF292C31),
                    contentColor = Color.White,
                    focusedContainerColor = MaterialTheme.colorScheme.primary,
                    focusedContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(if (kind == TvDownloadKind.VIDEO) "Video" else "Audio")
                }
            }
        }
    }
}

@Composable
private fun DownloadOptionRow(option: TvDownloadOption, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier),
        shape = ButtonDefaults.shape(shape),
        colors = ButtonDefaults.colors(
            containerColor = Color(0xFF292C31),
            contentColor = Color.White,
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            focusedContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(option.label, fontWeight = FontWeight.SemiBold)
                Text(option.detail, color = Color.White.copy(alpha = .62f), maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (option.recommended) Text("Recommended", color = MaterialTheme.colorScheme.primary)
                Text(option.size, color = Color.White.copy(alpha = .7f))
            }
        }
    }
}

@Composable
private fun DownloadProgress(job: DownloadJob, isSaving: Boolean, message: String?, error: String?) {
    val progress = when {
        message != null -> 1f
        isSaving -> 1f
        else -> (job.progressPercent ?: 0).coerceIn(0, 100) / 100f
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(downloadStatus(job, isSaving, message), color = Color.White, style = MaterialTheme.typography.titleLarge)
        Box(Modifier.fillMaxWidth().height(9.dp).background(Color.White.copy(alpha = .14f), RoundedCornerShape(99.dp))) {
            Box(Modifier.fillMaxWidth(progress).height(9.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(99.dp)))
        }
        Text(downloadDescription(job, message, error), color = Color.White.copy(alpha = .68f))
    }
}

@Composable
private fun DownloadJobActions(
    job: DownloadJob,
    isSaving: Boolean,
    message: String?,
    error: String?,
    focusRequester: FocusRequester,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        when {
            job.status == DownloadJobStatus.Queued || job.status == DownloadJobStatus.Running ->
                Button(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    colors = downloadActionColors(),
                ) {
                    CenteredButtonText("Cancel")
                }
            job.status == DownloadJobStatus.Done && error != null && !isSaving ->
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    colors = downloadActionColors(),
                ) {
                    CenteredButtonText("Try saving again")
                }
            message != null ->
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    colors = downloadActionColors(),
                ) {
                    CenteredButtonText("Done")
                }
            else -> Button(
                onClick = onClear,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                colors = downloadActionColors(),
            ) {
                CenteredButtonText("Choose another format")
            }
        }
    }
}

@Composable
private fun downloadActionColors() = ButtonDefaults.colors(
    containerColor = Color(0xFF292C31),
    contentColor = Color.White,
    focusedContainerColor = MaterialTheme.colorScheme.primary,
    focusedContentColor = MaterialTheme.colorScheme.onPrimary,
)

@Composable
private fun CenteredButtonText(label: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text(label) }
}

@Composable
private fun DownloadError(error: String) {
    Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 6.dp))
}

@Composable
private fun DownloadButtonContent(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Download, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}
