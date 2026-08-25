package dev.typetype.android.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.typetype.android.core.ui.theme.LocalTypeTypeAppearance

@Composable
fun TypeTypePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val appearance = LocalTypeTypeAppearance.current
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled && !isLoading,
        shape = MaterialTheme.shapes.medium,
        border = if (appearance.isManga) BorderStroke(2.dp, MaterialTheme.colorScheme.outline) else null,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(text = text, modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

@Composable
fun TypeTypeTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.secondary,
        ),
    ) {
        Text(text = text)
    }
}

@Composable
fun TypeTypeSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val appearance = LocalTypeTypeAppearance.current
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(if (appearance.isManga) 2.dp else 1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(text = text, modifier = Modifier.padding(vertical = 4.dp))
    }
}
