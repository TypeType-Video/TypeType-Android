package video.typetype.tv.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import video.typetype.tv.data.TvDestination
import video.typetype.tv.ui.theme.LocalTvAppearance
import video.typetype.tv.R

@Composable
internal fun TvTopNavigation(
    selected: TvDestination,
    onNavigate: (TvDestination) -> Unit,
    focusRequesterFor: (TvDestination) -> FocusRequester,
    contentFocusRequester: FocusRequester? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFF08090C),
                    .72f to Color(0xFF08090C),
                    1f to Color.Transparent,
                ),
            )
            .padding(start = 58.dp, top = 10.dp, end = 58.dp, bottom = 18.dp),
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_typetype),
                contentDescription = "TypeType",
                modifier = Modifier.size(34.dp),
            )
            Text(
                "TYPETYPE",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
            )
        }
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavigationTab("Home", TvDestination.HOME, selected, focusRequesterFor, contentFocusRequester, onNavigate)
            NavigationTab("Search", TvDestination.SEARCH, selected, focusRequesterFor, contentFocusRequester, onNavigate)
            NavigationTab("Library", TvDestination.LIBRARY, selected, focusRequesterFor, contentFocusRequester, onNavigate)
            NavigationTab("Settings", TvDestination.SETTINGS, selected, focusRequesterFor, contentFocusRequester, onNavigate)
        }
    }
}

@Composable
private fun NavigationTab(
    label: String,
    destination: TvDestination,
    selected: TvDestination,
    focusRequesterFor: (TvDestination) -> FocusRequester,
    contentFocusRequester: FocusRequester?,
    onNavigate: (TvDestination) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = focusRequesterFor(destination)
    val focused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        if (focused) 1.06f else 1f,
        tween(LocalTvAppearance.current.transitionMillis),
        label = "navigation-scale",
    )
    LaunchedEffect(destination == selected) {
        if (destination == selected) focusRequester.requestFocus()
    }
    val directionalFocus = if (destination == selected && contentFocusRequester != null) {
        Modifier.focusProperties { down = contentFocusRequester }
    } else {
        Modifier
    }
    Surface(
        modifier = Modifier.focusRequester(focusRequester).then(directionalFocus)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        onClick = { onNavigate(destination) },
        interactionSource = interactionSource,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (destination == selected) Color.White.copy(alpha = .14f) else Color.Transparent,
            focusedContainerColor = Color.White.copy(alpha = .24f),
        ),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 17.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (destination == selected) FontWeight.Bold else FontWeight.Medium,
                color = if (destination == selected || focused) {
                    Color.White
                } else {
                    Color.White.copy(alpha = .68f)
                },
            )
        }
    }
}
