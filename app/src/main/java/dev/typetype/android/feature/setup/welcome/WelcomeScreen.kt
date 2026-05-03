package dev.typetype.android.feature.setup.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.SectionHeader
import dev.typetype.android.core.ui.components.TypeTypePrimaryButton
import kotlinx.coroutines.flow.collectLatest

@Composable
fun WelcomeRoute(
    onNavigateToAddServer: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                WelcomeEvent.NavigateToAddServer -> onNavigateToAddServer()
            }
        }
    }
    WelcomeScreen(onAction = viewModel::onAction)
}

@Composable
fun WelcomeScreen(onAction: (WelcomeAction) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp, vertical = 48.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_monochrome),
                        contentDescription = null,
                        modifier = Modifier.size(96.dp),
                    )
                }
                Spacer(Modifier.height(24.dp))
                SectionHeader(text = "TypeType")
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Welcome aboard",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.weight(1f))
                TypeTypePrimaryButton(
                    text = "Get started",
                    onClick = { onAction(WelcomeAction.OnGetStartedClick) },
                )
            }
        }
    }
}
