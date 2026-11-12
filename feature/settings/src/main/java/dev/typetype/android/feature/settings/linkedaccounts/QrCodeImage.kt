package dev.typetype.android.feature.settings.linkedaccounts

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

@Composable
internal fun QrCodeImage(content: String, contentDescription: String, modifier: Modifier = Modifier) {
    val bitmap = remember(content) { encodeQr(content) }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = contentDescription,
        filterQuality = FilterQuality.None,
        modifier = modifier.background(Color.White).padding(12.dp).size(200.dp),
    )
}

// One pixel per module: the image is scaled without filtering, which keeps the code sharp and the bitmap tiny.
private fun encodeQr(content: String): Bitmap {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 0))
    val bitmap = createBitmap(matrix.width, matrix.height)
    for (x in 0 until matrix.width) {
        for (y in 0 until matrix.height) {
            bitmap[x, y] = if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
    }
    return bitmap
}
