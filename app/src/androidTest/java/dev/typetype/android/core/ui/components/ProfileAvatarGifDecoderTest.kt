package dev.typetype.android.core.ui.components

import android.content.Context
import android.graphics.drawable.Animatable
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.DrawableImage
import coil3.SingletonImageLoader
import coil3.gif.repeatCount
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileAvatarGifDecoderTest {
    @Test
    fun customGifAvatarUsesAnAnimatedDrawable() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val request = ImageRequest.Builder(context)
            .data(Base64.decode(ANIMATED_GIF, Base64.DEFAULT))
            .size(34)
            .repeatCount(Int.MAX_VALUE)
            .build()

        val result = SingletonImageLoader.get(context).execute(request)
        val drawable = (result as SuccessResult).image as DrawableImage

        assertTrue(drawable.drawable is Animatable)
    }

    private companion object {
        const val ANIMATED_GIF =
            "R0lGODlhAgACAPAAAP8AAAAAACH/C05FVFNDQVBFMi4wAwEAAAAh+QQAAAAAACwAAAAAAgACAAACAoRR" +
                "ACH5BAAAAAAALAAAAAACAAIAgAAA/wAAAAIChFEAOw=="
    }
}
