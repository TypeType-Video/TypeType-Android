package dev.typetype.android.feature.settings.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.SectionHeader
import dev.typetype.android.domain.preferences.AccentColor
import dev.typetype.android.feature.settings.SettingsDetailTopBar

private data class AccentSwatch(val accent: AccentColor, val labelRes: Int, val color: Color)

private val swatches = listOf(
    AccentSwatch(AccentColor.Red, R.string.accent_red, Color(0xFFEF4444)),
    AccentSwatch(AccentColor.Blue, R.string.accent_blue, Color(0xFF60A5FA)),
    AccentSwatch(AccentColor.Yellow, R.string.accent_yellow, Color(0xFFFBBF24)),
    AccentSwatch(AccentColor.Green, R.string.accent_green, Color(0xFF34D399)),
    AccentSwatch(AccentColor.Purple, R.string.accent_purple, Color(0xFFC084FC)),
    AccentSwatch(AccentColor.Violet, R.string.accent_violet, Color(0xFFA78BFA)),
    AccentSwatch(AccentColor.Monochrome, R.string.accent_monochrome, Color(0xFFE4E4E7)),
)

@Composable
fun AppearanceRoute(
    onNavigateBack: () -> Unit,
    viewModel: AppearanceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AppearanceScreen(
        currentAccent = state.accentColor,
        onAccentSelected = viewModel::selectAccent,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun AppearanceScreen(
    currentAccent: AccentColor,
    onAccentSelected: (AccentColor) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsDetailTopBar(
                title = stringResource(R.string.settings_appearance_title),
                onNavigateBack = onNavigateBack,
            )
            Spacer(Modifier.height(16.dp))
            SectionHeader(
                text = stringResource(R.string.settings_appearance_accent_color),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(swatches, key = { it.accent.name }) { swatch ->
                    AccentSwatchCell(
                        swatch = swatch,
                        isSelected = swatch.accent == currentAccent,
                        onClick = { onAccentSelected(swatch.accent) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AccentSwatchCell(
    swatch: AccentSwatch,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.selectable(
            selected = isSelected,
            role = Role.RadioButton,
            onClick = onClick,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(swatch.color)
                .border(
                    width = if (isSelected) 3.dp else 0.dp,
                    color = MaterialTheme.colorScheme.onBackground,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.Black,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(swatch.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
