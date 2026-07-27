package dev.typetype.android.core.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes

fun copyPlainText(
    context: Context,
    value: String,
    @StringRes labelRes: Int,
    @StringRes confirmationRes: Int,
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(labelRes), value))
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        Toast.makeText(context, confirmationRes, Toast.LENGTH_SHORT).show()
    }
}
