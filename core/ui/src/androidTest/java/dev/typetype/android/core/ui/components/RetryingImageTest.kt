package dev.typetype.android.core.ui.components

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import coil3.request.ImageRequest
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RetryingImageTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun failedImageRecoversWithoutChangingItsUrlOrLeavingTheScreen() {
        val file = File.createTempFile("retry-image", ".png", rule.activity.cacheDir)
        file.delete()
        val errors = AtomicInteger()
        val loaded = AtomicBoolean()
        val request = ImageRequest.Builder(rule.activity).data(file).build()
        try {
            rule.setContent {
                RetryingImage(
                    request = request,
                    contentDescription = "Test image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp),
                    onLoaded = { success ->
                        if (success) {
                            loaded.set(true)
                        } else {
                            errors.incrementAndGet()
                            val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
                            file.outputStream().use {
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                            }
                            bitmap.recycle()
                        }
                    },
                )
            }
            rule.waitUntil(10_000) { loaded.get() }
            assertEquals(1, errors.get())
        } finally {
            file.delete()
        }
    }
}
