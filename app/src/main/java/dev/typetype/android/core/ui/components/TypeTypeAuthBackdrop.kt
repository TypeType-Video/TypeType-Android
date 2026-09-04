package dev.typetype.android.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import dev.typetype.android.R

@Composable
fun TypeTypeAuthBackdrop(content: @Composable () -> Unit) {
    val background = MaterialTheme.colorScheme.background
    val dark = background.luminance() < 0.5f
    Box(Modifier.fillMaxSize().background(background)) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = -12f
                    scaleX = 1.18f
                    scaleY = 1.18f
                }
                .alpha(if (dark) 0.16f else 0.09f),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                repeat(7) {
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        repeat(12) {
                            Image(
                                painter = painterResource(R.drawable.ic_typetype_brand),
                                contentDescription = null,
                                modifier = Modifier.size(132.dp),
                            )
                        }
                    }
                }
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = if (dark) {
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.46f))
                    } else {
                        listOf(Color.Transparent, Color(0xFF64748B).copy(alpha = 0.34f))
                    },
                ),
            ),
        )
        Box(
            Modifier
                .size(300.dp)
                .offset(x = (-52).dp, y = 36.dp)
                .alpha(if (dark) 0.12f else 0.22f)
                .background(
                    Brush.radialGradient(listOf(Color(0xFF7DD3FC), Color.Transparent)),
                ),
        )
        Box(
            Modifier
                .size(340.dp)
                .offset(x = 36.dp, y = 440.dp)
                .alpha(if (dark) 0.10f else 0.18f)
                .background(
                    Brush.radialGradient(listOf(Color(0xFF22D3EE), Color.Transparent)),
                ),
        )
        content()
    }
}
